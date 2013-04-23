package com.sun.pdfview.decode;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;

//import org.jpedal.jbig2.JBIG2Decoder;
//import org.jpedal.jbig2.JBIG2Exception;

import com.levigo.jbig2.JBIG2Globals;
import com.levigo.jbig2.JBIG2ImageReader;
import com.levigo.jbig2.JBIG2ImageReaderSpi;
import com.levigo.jbig2.io.DefaultInputStreamFactory;
import com.sun.pdfview.PDFObject;

/*****************************************************************************
 * Decoder for jbig2 images within PDFs.
 * Copied from
 * https://pdf-renderer.dev.java.net/issues/show_bug.cgi?id=67
 *
 *  Problem is also described in:
 *	http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4799898
 * @since 17.11.2010
 ***************************************************************************
 */
public class JBig2Decode {	
	protected static ByteBuffer decode(PDFObject dict, ByteBuffer buf,
			PDFObject params) throws IOException {

		byte[] bytes = new byte[buf.remaining()];
		buf.get(bytes, 0, bytes.length);

		return ByteBuffer.wrap(decode(dict, bytes));
	}


//	protected static byte[] decode_JPEDAL(PDFObject dict, byte[] source) throws IOException {
//		JBIG2Decoder decoder;
//		decoder = new JBIG2Decoder();
//		try {
//			byte[] globals = getOptionFieldBytes(dict, "JBIG2Globals");
//			if (globals != null) {
//				decoder.setGlobalData(globals);
//			}
//			decoder.decodeJBIG2(source);
//		} catch (JBIG2Exception ex) {
//			IOException ioException;
//
//			ioException = new IOException();
//			ioException.initCause(ex);
//			throw ioException;
//		}
//		return decoder.getPageAsJBIG2Bitmap(0).getData(true);
//	}

	protected static byte[] decode(PDFObject dict, byte[] source) throws IOException {
		DefaultInputStreamFactory disf = new DefaultInputStreamFactory();
		ImageInputStream imageInputStream = disf.getInputStream(new ByteArrayInputStream(source));
		JBIG2ImageReader imageReader = new JBIG2ImageReader(new JBIG2ImageReaderSpi());
		// Set input
		imageReader.setInput(imageInputStream);
		// Getting globals and convert it from byte[]
		byte[] globalsA = getOptionFieldBytes(dict, "JBIG2Globals");
		if (globalsA != null) {
			JBIG2Globals globals = imageReader.processGlobals(ImageIO.createImageInputStream(new ByteArrayInputStream(globalsA)));
			// Setting globals
			imageReader.setGlobals(globals);
		}
		BufferedImage img = imageReader.read(0);
		// aufraeumen
		imageInputStream.close();
		imageReader.dispose();
		// Farbtiefe ändern
		BufferedImage out = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
		out.getGraphics().drawImage(img, 0, 0, null);
		// convert to byte[] and return it
		WritableRaster raster = out.getRaster();
		DataBufferByte data = (DataBufferByte) raster.getDataBuffer();
		return ( data.getData() );
	}	
	
	public static byte[] getOptionFieldBytes(PDFObject dict, String name) throws IOException {

		PDFObject dictParams =  dict.getDictRef("DecodeParms");

		if (dictParams == null) {
			return null;
		}
		PDFObject value = dictParams.getDictRef(name);
		if (value == null) {
			return null;
		}
		return value.getStream();
	}

}