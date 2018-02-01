package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Sharpen;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.operation.redaction.Redaction;
import edu.illinois.library.cantaloupe.operation.overlay.ImageOverlay;
import edu.illinois.library.cantaloupe.operation.overlay.Position;
import edu.illinois.library.cantaloupe.operation.overlay.StringOverlay;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static edu.illinois.library.cantaloupe.test.Assert.ImageAssert.*;
import static org.junit.Assert.*;

public class Java2DUtilTest extends BaseTest {

    private BufferedImage newColorImage(int componentSize, boolean hasAlpha) {
        return newColorImage(20, 20, componentSize, hasAlpha);
    }

    private BufferedImage newColorImage(int width, int height,
                                        int componentSize, boolean hasAlpha) {
        if (componentSize <= 8) {
            int type = hasAlpha ?
                    BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR;
            return new BufferedImage(width, height, type);
        }
        final ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        final boolean isAlphaPremultiplied = false;
        final int transparency = (hasAlpha) ?
                Transparency.TRANSLUCENT : Transparency.OPAQUE;
        final int dataType = DataBuffer.TYPE_USHORT;
        final ColorModel colorModel = new ComponentColorModel(
                colorSpace, hasAlpha, isAlphaPremultiplied, transparency,
                dataType);
        final WritableRaster raster =
                colorModel.createCompatibleWritableRaster(width, height);
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

    private BufferedImage newGrayImage(int componentSize, boolean hasAlpha) {
        return newGrayImage(20, 20, componentSize, hasAlpha);
    }

    private BufferedImage newGrayImage(int width, int height,
                                       int componentSize, boolean hasAlpha) {
        if (!hasAlpha) {
            int type = (componentSize > 8) ?
                    BufferedImage.TYPE_USHORT_GRAY : BufferedImage.TYPE_BYTE_GRAY;
            return new BufferedImage(width, height, type);
        }
        final ColorSpace colorSpace = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        final int[] componentSizes = new int[] { componentSize, componentSize };
        final boolean isAlphaPremultiplied = false;
        final int transparency = Transparency.TRANSLUCENT;
        final int dataType = (componentSize > 8) ?
                DataBuffer.TYPE_USHORT : DataBuffer.TYPE_BYTE;
        final ColorModel colorModel = new ComponentColorModel(
                colorSpace, componentSizes, hasAlpha, isAlphaPremultiplied,
                transparency, dataType);
        final WritableRaster raster =
                colorModel.createCompatibleWritableRaster(width, height);
        return new BufferedImage(colorModel, raster, isAlphaPremultiplied, null);
    }

    /* applyRedactions() */

    @Test
    public void testApplyRedactions() {
        final BufferedImage baseImage = newColorImage(64, 56, 8, false);
        final ReductionFactor rf = new ReductionFactor(0);

        // fill it with white
        Graphics2D g2d = baseImage.createGraphics();
        g2d.setColor(Color.WHITE.toColor());
        g2d.fillRect(0, 0, baseImage.getWidth(), baseImage.getHeight());
        g2d.dispose();

        // create some Redactions
        List<Redaction> redactions = new ArrayList<>();
        redactions.add(new Redaction(new Rectangle(0, 0, 20, 20)));
        redactions.add(new Redaction(new Rectangle(20, 20, 20, 20)));
        final Crop crop = new Crop(0, 0, baseImage.getWidth(),
                baseImage.getTileHeight());

        // apply them
        final BufferedImage redactedImage = Java2DUtil.applyRedactions(
                baseImage, crop, rf, redactions);

        // test for the first one
        assertRGBA(redactedImage.getRGB(0, 0), 0, 0, 0, 255);

        // test for the second one
        assertRGBA(redactedImage.getRGB(25, 25), 0, 0, 0, 255);
    }

    /* applyOverlay() */

    @Test
    public void testApplyOverlayWithImageOverlay() throws Exception {
        final BufferedImage baseImage = newColorImage(8, false);

        // fill it with white
        Graphics2D g2d = baseImage.createGraphics();
        g2d.setColor(Color.WHITE.toColor());
        g2d.fillRect(0, 0, baseImage.getWidth(), baseImage.getHeight());
        g2d.dispose();

        // create an Overlay
        final ImageOverlay overlay = new ImageOverlay(
                TestUtil.getImage("png-rgb-1x1x8.png").toUri(),
                Position.TOP_LEFT, 0);

        // apply it
        final BufferedImage overlaidImage = Java2DUtil.applyOverlay(
                baseImage, overlay);

        assertRGBA(overlaidImage.getRGB(0, 0), 0, 0, 0, 255);
    }

    @Test
    public void testApplyOverlayWithImageOverlayAndInset() throws Exception {
        final BufferedImage baseImage = newColorImage(8, false);

        // fill it with white
        Graphics2D g2d = baseImage.createGraphics();
        g2d.setColor(Color.WHITE.toColor());
        g2d.fillRect(0, 0, baseImage.getWidth(), baseImage.getHeight());
        g2d.dispose();

        // create a Overlay
        final int inset = 2;
        final ImageOverlay overlay = new ImageOverlay(
                TestUtil.getImage("png-rgb-1x1x8.png").toUri(),
                Position.BOTTOM_RIGHT,
                inset);

        // apply it
        final BufferedImage overlaidImage = Java2DUtil.applyOverlay(
                baseImage, overlay);

        int pixel = overlaidImage.getRGB(
                baseImage.getWidth() - inset - 1,
                baseImage.getHeight() - inset - 1);
        assertRGBA(pixel, 0, 0, 0, 255);
    }

    @Test
    @Ignore // TODO: see inline todo
    public void testApplyOverlayWithStringOverlay() throws Exception {
        final BufferedImage baseImage = newColorImage(8, false);

        // create a StringOverlay
        final StringOverlay overlay = new StringOverlay(
                "X", Position.TOP_LEFT, 0,
                new Font("SansSerif", Font.PLAIN, 12), 11,
                Color.WHITE, Color.BLACK, Color.WHITE, 0f);

        // apply it
        final BufferedImage overlaidImage = Java2DUtil.applyOverlay(
                baseImage, overlay);

        // Test the background color
        assertRGBA(overlaidImage.getRGB(2, 2), 0, 0, 0, 255);

        // Test the font color TODO: this pixel will be different colors on different JVMs and/or with different versions of the SansSerif font
        int pixel = overlaidImage.getRGB(9, 8);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertTrue(red > 240);
        assertTrue(green > 240);
        assertTrue(blue > 240);
    }

    /* cropImage() */

    @Test
    public void testCropImage() {
        final float fudge = 0.0000001f;
        final int width = 200, height = 100;
        BufferedImage inImage = newColorImage(width, height, 8, false);
        BufferedImage outImage;

        // full
        Crop crop = new Crop();
        crop.setFull(true);
        outImage = Java2DUtil.cropImage(inImage, crop);
        assertSame(inImage, outImage);

        // square crop
        crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        crop.setWidth(50f);
        crop.setHeight(50f);
        outImage = Java2DUtil.cropImage(inImage, crop);
        assertEquals(height, outImage.getWidth());
        assertEquals(height, outImage.getHeight());

        // pixel crop
        crop = new Crop();
        crop.setWidth(50f);
        crop.setHeight(50f);
        outImage = Java2DUtil.cropImage(inImage, crop);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // percentage crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.5f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        outImage = Java2DUtil.cropImage(inImage, crop);
        assertEquals(width * 0.5f, outImage.getWidth(), fudge);
        assertEquals(height * 0.5f, outImage.getHeight(), fudge);
    }

    @Test
    public void testCropImageWithReductionFactor() {
        final float fudge = 0.0000001f;
        final int width = 100, height = 100;
        BufferedImage inImage = newColorImage(width, height, 8, false);
        BufferedImage outImage;

        // full crop
        Crop crop = new Crop();
        crop.setFull(true);
        ReductionFactor rf = new ReductionFactor(1);
        outImage = Java2DUtil.cropImage(inImage, crop, rf);
        assertSame(inImage, outImage);

        // pixel crop
        crop = new Crop();
        crop.setWidth(width / 2f);
        crop.setHeight(height / 2f);
        rf = new ReductionFactor(1);
        outImage = Java2DUtil.cropImage(inImage, crop, rf);
        assertEquals(width / 4f, outImage.getWidth(), fudge);
        assertEquals(height / 4f, outImage.getHeight(), fudge);

        // percentage crop
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.5f);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        rf = new ReductionFactor(1);
        outImage = Java2DUtil.cropImage(inImage, crop, rf);
        assertEquals(width / 4f, outImage.getWidth(), fudge);
        assertEquals(height / 4f, outImage.getHeight(), fudge);
    }

    /* getOverlayImage() */

    @Test
    public void testGetOverlayImage() throws Exception {
        ImageOverlay overlay = new ImageOverlay(
                TestUtil.getImage("png").toUri(), Position.BOTTOM_RIGHT, 0);
        assertNotNull(Java2DUtil.getOverlayImage(overlay));
    }

    /* reduceTo8Bits() */

    @Test
    public void testReduceTo8BitsWith8BitGray() {
        BufferedImage image = newGrayImage(8, false);
        BufferedImage result = Java2DUtil.reduceTo8Bits(image);
        assertSame(image, result);
    }

    @Test
    public void testReduceTo8BitsWith8BitRGBA() {
        BufferedImage image = newColorImage(8, true);
        BufferedImage result = Java2DUtil.reduceTo8Bits(image);
        assertSame(image, result);
    }

    @Test
    public void testReduceTo8BitsWith16BitGray() {
        BufferedImage image = newGrayImage(16, false);
        BufferedImage result = Java2DUtil.reduceTo8Bits(image);
        assertEquals(8, result.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, result.getType());
    }

    @Test
    public void testReduceTo8BitsWith16BitRGBA() {
        BufferedImage image = newColorImage(16, true);
        BufferedImage result = Java2DUtil.reduceTo8Bits(image);
        assertEquals(8, result.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_INT_ARGB, result.getType());
    }

    /* removeAlpha(BufferedImage) */

    @Test
    public void testRemoveAlphaOn8BitGrayImage() {
        BufferedImage inImage = newGrayImage(8, false);
        assertFalse(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage);
        assertSame(inImage, outImage);
    }

    @Test
    public void testRemoveAlphaOn8BitGrayImageWithAlpha() {
        BufferedImage inImage = newGrayImage(8, true);
        assertTrue(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage);
        assertFalse(outImage.getColorModel().hasAlpha());
    }

    @Test
    public void testRemoveAlphaOn8BitRGBImage() {
        BufferedImage inImage = newColorImage(8, false);
        assertFalse(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage);
        assertSame(inImage, outImage);
    }

    @Test
    public void testRemoveAlphaOn8BitRGBAImage() {
        BufferedImage inImage = newColorImage(16, true);
        assertTrue(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage);
        assertFalse(outImage.getColorModel().hasAlpha());
    }

    @Test
    public void testRemoveAlphaOn16BitGrayImage() {
        BufferedImage inImage = newGrayImage(16, false);
        assertFalse(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage);
        assertSame(inImage, outImage);
    }

    @Test
    public void testRemoveAlphaOn16BitGrayImageWithAlpha() {
        BufferedImage inImage = newGrayImage(16, true);
        assertTrue(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage);
        assertFalse(outImage.getColorModel().hasAlpha());
    }

    @Test
    public void testRemoveAlphaOn16BitRGBImage() {
        BufferedImage inImage = newColorImage(16, false);
        assertFalse(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage);
        assertSame(inImage, outImage);
    }

    @Test
    public void testRemoveAlphaOn16BitRGBAImage() {
        BufferedImage inImage = newColorImage(16, true);
        assertTrue(inImage.getColorModel().hasAlpha());

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage);
        assertFalse(outImage.getColorModel().hasAlpha());
    }

    /* removeAlpha(BufferedImage, Color) */

    @Test
    public void testRemoveAlphaOnImageWithAlphaWithBackgroundColor()
            throws IOException {
        Path file = TestUtil.getImage("png-rgba-64x56x8.png");
        BufferedImage inImage = ImageIO.read(file.toFile());
        assertTrue(inImage.getColorModel().hasAlpha());

        int[] rgba = { 0 };
        inImage.getAlphaRaster().setPixel(0, 0, rgba);

        BufferedImage outImage = Java2DUtil.removeAlpha(inImage, Color.RED);

        int[] expected = new int[] {255, 0, 0, 0};
        int[] actual = new int[4];
        assertArrayEquals(expected, outImage.getRaster().getPixel(0, 0, actual));
    }

    /* rotateImage() */

    @Test
    public void testRotateImageDimensions() {
        BufferedImage inImage = newColorImage(8, false);
        final int sourceWidth = inImage.getWidth();
        final int sourceHeight = inImage.getHeight();

        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtil.rotateImage(inImage, rotate);

        final double radians = Math.toRadians(rotate.getDegrees());
        final int expectedWidth = (int) Math.round(Math.abs(sourceWidth *
                Math.cos(radians)) + Math.abs(sourceHeight *
                Math.sin(radians)));
        final int expectedHeight = (int) Math.round(Math.abs(sourceHeight *
                Math.cos(radians)) + Math.abs(sourceWidth *
                Math.sin(radians)));

        assertEquals(expectedWidth, outImage.getWidth());
        assertEquals(expectedHeight, outImage.getHeight());
    }

    @Test
    public void testRotateImageWith8BitGray() {
        BufferedImage inImage = newGrayImage(8, false);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtil.rotateImage(inImage, rotate);

        assertFalse(outImage.getColorModel().hasAlpha());
        assertEquals(8, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, outImage.getType());
    }

    @Test
    public void testRotateImageWith8BitGrayWithAlpha() {
        BufferedImage inImage = newGrayImage(8, true);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtil.rotateImage(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(8, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_CUSTOM, outImage.getType());
    }

    @Test
    public void testRotateImageWith8BitRGB() {
        BufferedImage inImage = newColorImage(8, false);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtil.rotateImage(inImage, rotate);

        assertFalse(outImage.getColorModel().hasAlpha());
        assertEquals(8, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_3BYTE_BGR, outImage.getType());
    }

    @Test
    public void testRotateImageWith8BitRGBA() {
        BufferedImage inImage = newColorImage(8, true);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtil.rotateImage(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(8, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_4BYTE_ABGR, outImage.getType());
    }

    @Test
    public void testRotateImageWith16BitGray() {
        BufferedImage inImage = newGrayImage(16, false);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtil.rotateImage(inImage, rotate);

        assertFalse(outImage.getColorModel().hasAlpha());
        assertEquals(16, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_USHORT_GRAY, outImage.getType());
    }

    @Test
    public void testRotateImageWith16BitGrayWithAlpha() {
        BufferedImage inImage = newGrayImage(16, true);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtil.rotateImage(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(16, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_CUSTOM, outImage.getType());
    }

    @Test
    public void testRotateImageWith16BitRGB() {
        BufferedImage inImage = newColorImage(16, false);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtil.rotateImage(inImage, rotate);

        assertFalse(outImage.getColorModel().hasAlpha());
        assertEquals(16, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_CUSTOM, outImage.getType());
    }

    @Test
    public void testRotateImageWith16BitRGBA() {
        BufferedImage inImage = newColorImage(16, true);
        Rotate rotate = new Rotate(15);
        BufferedImage outImage = Java2DUtil.rotateImage(inImage, rotate);

        assertTrue(outImage.getColorModel().hasAlpha());
        assertEquals(16, outImage.getColorModel().getComponentSize(0));
        assertEquals(BufferedImage.TYPE_CUSTOM, outImage.getType());
    }

    /* scaleImage(BufferedImage, Scale) */

    @Test
    public void testScaleImage() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);
        BufferedImage outImage;

        // Scale.Mode.FULL
        Scale scale = new Scale();
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertSame(inImage, outImage);

        // Scale.Mode.ASPECT_FIT_WIDTH
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(50);
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_HEIGHT
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(50);
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_INSIDE
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(50);
        scale.setHeight(50);
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.NON_ASPECT_FILL
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        scale.setWidth(80);
        scale.setHeight(50);
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertEquals(80, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // scale-by percent
        scale = new Scale(0.25f);
        outImage = Java2DUtil.scaleImage(inImage, scale);
        assertEquals(25, outImage.getWidth());
        assertEquals(25, outImage.getHeight());
    }

    /* scaleImage(BufferedImage, Scale, ReductionFactor) */

    @Test
    public void testScaleImageWithReductionFactor() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);
        BufferedImage outImage;

        // Scale.Mode.ASPECT_FIT_WIDTH
        Scale scale = new Scale(50, null, Scale.Mode.ASPECT_FIT_WIDTH);
        ReductionFactor rf = new ReductionFactor(1);
        outImage = Java2DUtil.scaleImage(inImage, scale, rf);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_HEIGHT
        scale = new Scale(null, 50, Scale.Mode.ASPECT_FIT_HEIGHT);
        rf = new ReductionFactor(1);
        outImage = Java2DUtil.scaleImage(inImage, scale, rf);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // Scale.Mode.ASPECT_FIT_INSIDE
        scale = new Scale(50, 50, Scale.Mode.ASPECT_FIT_INSIDE);
        rf = new ReductionFactor(1);
        outImage = Java2DUtil.scaleImage(inImage, scale, rf);
        assertEquals(50, outImage.getWidth());
        assertEquals(50, outImage.getHeight());

        // scale-by-percent
        scale = new Scale(0.25f);
        rf = new ReductionFactor(2);
        outImage = Java2DUtil.scaleImage(inImage, scale, rf);
        assertEquals(100, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

    /* sharpenImage() */

    @Test
    public void testSharpenImage() {
        BufferedImage inImage = newColorImage(20, 20, 8, false);
        Sharpen sharpen = new Sharpen(0.1f);
        BufferedImage outImage = Java2DUtil.sharpenImage(inImage, sharpen);

        assertEquals(20, outImage.getWidth());
        assertEquals(20, outImage.getHeight());
    }

    /* stretchContrast() */

    @Test
    public void testStretchContrast() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);
        final Rectangle leftHalf = new Rectangle(0, 0, 50, 100);
        final Rectangle rightHalf = new Rectangle(50, 0, 50, 100);

        final Graphics2D g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.DARK_GRAY);
        g2d.fill(leftHalf);
        g2d.setColor(java.awt.Color.LIGHT_GRAY);
        g2d.fill(rightHalf);

        BufferedImage outImage = Java2DUtil.stretchContrast(inImage);

        assertEquals(-16777216, outImage.getRGB(10, 10));
        assertEquals(-1, outImage.getRGB(90, 90));
    }

    /* transformColor() */

    @Test
    public void testTransformColorFrom8BitRGBToBitonal() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);
        BufferedImage outImage;

        // Create a cyan image.
        Graphics2D g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.CYAN);
        g2d.fill(new Rectangle(0, 0, 100, 100));
        g2d.dispose();

        // Transform to bitonal.
        outImage = Java2DUtil.transformColor(inImage, ColorTransform.BITONAL);

        // Expect it to be transformed to white.
        assertRGBA(outImage.getRGB(0, 0), 255, 255, 255, 255);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, outImage.getType());

        // Create a red image.
        g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.RED);
        g2d.fill(new Rectangle(0, 0, 100, 100));
        g2d.dispose();

        // Transform to bitonal.
        outImage = Java2DUtil.transformColor(inImage, ColorTransform.BITONAL);

        // Expect it to be transformed to black.
        assertRGBA(outImage.getRGB(0, 0), 0, 0, 0, 255);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, outImage.getType());
    }

    @Test
    public void testTransformColorFrom16BitRGBAToBitonal() {
        BufferedImage inImage = newColorImage(16, true);

        // Create a cyan image.
        Graphics2D g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.CYAN);
        g2d.fill(new Rectangle(0, 0, 100, 100));
        g2d.dispose();

        // Transform to bitonal.
        BufferedImage outImage = Java2DUtil.transformColor(inImage,
                ColorTransform.BITONAL);

        // Expect it to be transformed to white.
        assertRGBA(outImage.getRGB(0, 0), 255, 255, 255, 255);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, outImage.getType());

        // Create a red image.
        g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.RED);
        g2d.fill(new Rectangle(0, 0, 100, 100));
        g2d.dispose();

        // Transform to bitonal.
        outImage = Java2DUtil.transformColor(inImage, ColorTransform.BITONAL);

        // Expect it to be transformed to black.
        assertRGBA(outImage.getRGB(0, 0), 0, 0, 0, 255);
        assertEquals(BufferedImage.TYPE_BYTE_BINARY, outImage.getType());
    }

    @Test
    public void testTransformColorFrom8BitRGBToGray() {
        BufferedImage inImage = newColorImage(100, 100, 8, false);

        // Start with a red image.
        Graphics2D g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.RED);
        g2d.fill(new Rectangle(0, 0, 100, 100));
        g2d.dispose();

        // Transform to grayscale.
        BufferedImage outImage = Java2DUtil.transformColor(inImage,
                ColorTransform.GRAY);

        assertGray(outImage.getRGB(0, 0));
        assertEquals(BufferedImage.TYPE_BYTE_GRAY, outImage.getType());
    }

    @Test
    public void testTransformColorFrom16BitRGBAToGray() {
        BufferedImage inImage = newColorImage(100, 100, 16, true);

        // Start with a red image.
        Graphics2D g2d = inImage.createGraphics();
        g2d.setColor(java.awt.Color.RED);
        g2d.fill(new Rectangle(0, 0, 100, 100));
        g2d.dispose();

        // Transform to grayscale.
        BufferedImage outImage = Java2DUtil.transformColor(inImage,
                ColorTransform.GRAY);

        assertGray(outImage.getRGB(0, 0));
        assertEquals(BufferedImage.TYPE_USHORT_GRAY, outImage.getType());
        assertEquals(16, outImage.getColorModel().getComponentSize(0));
    }

    @Test
    public void testTransformColorFromBitonalToBitonal() {
        BufferedImage inImage = new BufferedImage(100, 100,
                BufferedImage.TYPE_BYTE_BINARY);
        BufferedImage outImage = Java2DUtil.transformColor(inImage,
                ColorTransform.BITONAL);
        assertSame(inImage, outImage);
    }

    @Test
    public void testTransformColorFromGrayToGray() {
        BufferedImage inImage = newGrayImage(100, 100, 8, false);
        BufferedImage outImage = Java2DUtil.transformColor(inImage,
                ColorTransform.GRAY);
        assertSame(inImage, outImage);
    }

    @Test
    public void testTransformColorFromGrayAlphaToGray() {
        BufferedImage inImage = newGrayImage(100, 100, 8, true);
        BufferedImage outImage = Java2DUtil.transformColor(inImage,
                ColorTransform.GRAY);
        assertSame(inImage, outImage);
    }

    /* transposeImage() */

    @Test
    public void testTransposeImage() {
        BufferedImage inImage = newColorImage(200, 100, 8, false);
        Transpose transpose = Transpose.HORIZONTAL;
        BufferedImage outImage = Java2DUtil.transposeImage(inImage, transpose);

        assertEquals(200, outImage.getWidth());
        assertEquals(100, outImage.getHeight());
    }

}
