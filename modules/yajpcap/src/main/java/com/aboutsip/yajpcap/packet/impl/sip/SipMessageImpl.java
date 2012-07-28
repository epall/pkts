/**
 * 
 */
package com.aboutsip.yajpcap.packet.impl.sip;

import java.util.HashMap;
import java.util.Map;

import com.aboutsip.buffer.Buffer;
import com.aboutsip.buffer.Buffers;
import com.aboutsip.yajpcap.packet.SipHeader;
import com.aboutsip.yajpcap.packet.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 * 
 */
public abstract class SipMessageImpl implements SipMessage {

    public static final Buffer FROM_HEADER = Buffers.wrap("From".getBytes());

    public static final Buffer TO_HEADER = Buffers.wrap("To".getBytes());

    public static final Buffer Call_ID_HEADER = Buffers.wrap("Call-ID".getBytes());

    public static final Buffer CSEQ_HEADER = Buffers.wrap("CSeq".getBytes());

    /**
     * The initial line of the sip message, which is either a request or a
     * response line
     */
    private final SipInitialLine initialLine;

    /**
     * All the headers of the sip message
     */
    private final Buffer headers;

    /**
     * The payload, which may be null
     */
    private final Buffer payload;

    /**
     * Map with parsed headers. Need to change since there are many headers that
     * can appear multiple times. We'll get to that...
     * 
     * We'll keep the default size of 16 and load factory of 0.75, which means
     * that we won't do a re-hash until we hit 12 headers. A basic request has
     * around 10ish headers but in real life there will be much more so get some
     * real world examples and set an appropriate size based on that.
     */
    private final Map<Buffer, SipHeader> parsedHeaders = new HashMap<Buffer, SipHeader>(16, 0.75f);

    /**
     * 
     * @param initialLine the initial line, which is either a request or a
     *            response line
     * @param headers all the headers (un-parsed) of the SIP message
     * @param payload the payload or null if there is none
     */
    public SipMessageImpl(final SipInitialLine initialLine, final Buffer headers, final Buffer payload) {
        assert initialLine != null;
        assert headers != null;

        this.initialLine = initialLine;
        this.headers = headers;
        this.payload = payload;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isResponse() {
        return this.initialLine.isResponseLine();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isRequest() {
        return this.initialLine.isRequestLine();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SipHeader getHeader(final Buffer headerName) throws SipParseException {
        final SipHeader h = this.parsedHeaders.get(headerName);
        if (h != null) {
            return h;
        }

        while (this.headers.hasReadableBytes()) {
            final SipHeader header = SipParser.nextHeader(this.headers);
            this.parsedHeaders.put(header.getName(), header);
            if (header.getName().equals(headerName)) {
                return header;
            }
        }

        // didn't find the header that was requested
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SipHeader getFromHeader() throws SipParseException {
        return getHeader(FROM_HEADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SipHeader getToHeader() throws SipParseException {
        return getHeader(TO_HEADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SipHeader getCallIDHeader() throws SipParseException {
        return getHeader(Call_ID_HEADER);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public abstract Buffer getMethod() throws SipParseException;

    /**
     * {@inheritDoc}
     */
    @Override
    public void verify() {
        // TODO Auto-generated method stub
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.initialLine.toString()).append("\n");
        sb.append(this.headers.toString()).append("\n");
        if (this.payload != null) {
            sb.append(this.payload.toString()).append("\n");
        }
        return sb.toString();
    }

}
