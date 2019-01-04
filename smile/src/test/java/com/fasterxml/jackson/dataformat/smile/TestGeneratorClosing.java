package com.fasterxml.jackson.dataformat.smile;


import com.fasterxml.jackson.core.*;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Borrowed from jackson-core.
 *
 * Set of basic unit tests that verify aspect of closing a
 * {@link JsonGenerator} instance. This includes both closing
 * of physical resources (target), and logical content
 * (json content tree)
 *<p>
 * Specifically, features
 * <code>JsonGenerator.Feature#AUTO_CLOSE_TARGET</code>
 * and
 * <code>JsonGenerator.Feature.AUTO_CLOSE_JSON_CONTENT</code>
 * are tested.
 */
public class TestGeneratorClosing extends BaseTestForSmile
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    final static class MyStream extends ByteArrayOutputStream
    {
        boolean _isClosed = false;
        boolean _isFlushed = false;

        public MyStream() { }

        @Override
        public void flush() throws IOException {
            _isFlushed = true;
            super.flush();
        }
        @Override
        public void close() throws IOException {
            _isClosed = true;
            super.close();
        }
        public boolean isClosed() { return _isClosed; }
        public boolean isFlushed() { return _isFlushed; }
    }

    static class MyBytes extends ByteArrayOutputStream
    {
        public int flushed = 0;

        @Override
        public void flush() throws IOException
        {
            ++flushed;
            super.flush();
        }
    }

    private TokenStreamFactory.TSFBuilder<?,?> newFactoryBuilder() {
        return SmileFactory.builder();
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    /**
     * This unit test checks the default behaviour; with no auto-close, no
     * automatic closing should occur, nor explicit one unless specific
     * forcing method is used.
     */
    public void testNoAutoCloseGenerator() throws Exception
    {
        TokenStreamFactory f = newFactoryBuilder().build();

        // Check the default settings
        TestCase.assertTrue(f.isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET));
        // then change
        f = f.rebuild().disable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();
        TestCase.assertFalse(f.isEnabled(StreamWriteFeature.AUTO_CLOSE_TARGET));
        @SuppressWarnings("resource")
        MyStream output = new MyStream();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output);

        // shouldn't be closed to begin with...
        TestCase.assertFalse(output.isClosed());
        g.writeNumber(39);
        // regular close won't close it either:
        g.close();
        TestCase.assertFalse(output.isClosed());
    }

    public void testCloseGenerator() throws Exception
    {
        TokenStreamFactory f = newFactoryBuilder()
                .enable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();
        @SuppressWarnings("resource")
        MyStream output = new MyStream();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output);

        // shouldn't be closed to begin with...
        TestCase.assertFalse(output.isClosed());
        g.writeNumber(39);
        // but close() should now close the writer
        g.close();
        TestCase.assertTrue(output.isClosed());
    }

    public void testNoAutoCloseOutputStream() throws Exception
    {
        TokenStreamFactory f = newFactoryBuilder()
                .disable(StreamWriteFeature.AUTO_CLOSE_TARGET).build();
        @SuppressWarnings("resource")
        MyStream output = new MyStream();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output, JsonEncoding.UTF8);

        TestCase.assertFalse(output.isClosed());
        g.writeNumber(39);
        g.close();
        TestCase.assertFalse(output.isClosed());
        TestCase.assertTrue(output.isFlushed());
    }

    public void testNoAutoCloseOrFlushOutputStream() throws Exception
    {
        TokenStreamFactory f = newFactoryBuilder()
                .disable(StreamWriteFeature.AUTO_CLOSE_TARGET)
                .disable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)
                .build();
        @SuppressWarnings("resource")
        MyStream output = new MyStream();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), output, JsonEncoding.UTF8);

        TestCase.assertFalse(output.isClosed());
        g.writeNumber(39);
        g.close();
        TestCase.assertFalse(output.isClosed());
        TestCase.assertFalse(output.isFlushed());
    }

    // [JACKSON-401]
    @SuppressWarnings("resource")
    public void testAutoFlushOrNot() throws Exception
    {
        TokenStreamFactory f = newFactoryBuilder().build();
        TestCase.assertTrue(f.isEnabled(StreamWriteFeature.FLUSH_PASSED_TO_STREAM));
        
        // ditto with stream
        MyBytes bytes = new MyBytes();
        JsonGenerator g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        g.writeStartArray();
        g.writeEndArray();
        TestCase.assertEquals(0, bytes.flushed);
        g.flush();
        TestCase.assertEquals(1, bytes.flushed);
        TestCase.assertEquals(6, bytes.toByteArray().length);
        g.close();

        // then disable and we should not see flushing again...
        f = f.rebuild()
            .disable(StreamWriteFeature.FLUSH_PASSED_TO_STREAM)
            .build();

        // and then with OutputStream
        bytes = new MyBytes();
        g = f.createGenerator(ObjectWriteContext.empty(), bytes, JsonEncoding.UTF8);
        g.writeStartArray();
        g.writeEndArray();
        TestCase.assertEquals(0, bytes.flushed);
        g.flush();
        TestCase.assertEquals(0, bytes.flushed);
        g.close();
        TestCase.assertEquals(0, bytes.flushed);
        TestCase.assertEquals(6, bytes.toByteArray().length);
    }
}