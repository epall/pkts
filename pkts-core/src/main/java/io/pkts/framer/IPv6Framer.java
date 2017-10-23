/**
 *
 */
package io.pkts.framer;

import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.IPv6Packet;
import io.pkts.packet.MACPacket;
import io.pkts.packet.impl.IPv6PacketImpl;
import io.pkts.protocol.Protocol;

import java.io.IOException;

/**
 * @author jonas@jonasborjesson.com
 *
 */
public class IPv6Framer implements Framer<MACPacket, IPv6Packet> {

    public IPv6Framer() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Protocol getProtocol() {
        return Protocol.IPv6;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPv6Packet frame(final MACPacket parent, final Buffer payload) throws IOException {
        if (parent == null) {
            throw new IllegalArgumentException("The parent frame cannot be null");
        }

        final Buffer fixedHeader = payload.readBytes(IPv6PacketImpl.FIXED_HEADER_LENGTH);

        // byte 1, contains the version and the length
        final int version = (fixedHeader.getByte(0) & 0xF0) >> 4;
        if (version != IPv6Packet.VERSION_IDENTIFIER) {
            throw new FramingException(String.format("Invalid IPv6 version: %d", version), Protocol.IPv6);
        }

        final int payloadLength = fixedHeader.getUnsignedShort(4);

        int nextHeader = fixedHeader.getByte(6);

        final Buffer extensionHeadersBuffer = Buffers.createBuffer(400);
        while (nextHeader == IPv6Packet.EXTENSION_HOP_BY_HOP ||
                nextHeader == IPv6Packet.EXTENSION_DESTINATION_OPTIONS ||
                nextHeader == IPv6Packet.EXTENSION_ROUTING ||
                nextHeader == IPv6Packet.EXTENSION_FRAGMENT ||
                nextHeader == IPv6Packet.EXTENSION_AH ||
                nextHeader == IPv6Packet.EXTENSION_ESP ||
                nextHeader == IPv6Packet.EXTENSION_MOBILITY) {
            nextHeader = accumulateNextHeader(nextHeader, payload, extensionHeadersBuffer);
        }
        // TODO: extract actual PayloadLength from Hop-by-Hop extension header, if present


        //Trim off any padding from the upper layer, e.g. Ethernet padding for small packets.
        // If the captured frame was truncated, then use the truncated size for the data buffer, instead of what the
        // IPv6 header says its length should be.
        final int totalLength = IPv6PacketImpl.FIXED_HEADER_LENGTH + extensionHeadersBuffer.getWriterIndex() + payloadLength;
        final Buffer data = payload.slice(Math.min(totalLength, payload.capacity()));
        return new IPv6PacketImpl(parent, Buffers.wrap(fixedHeader, extensionHeadersBuffer), nextHeader, data);
    }

    private int accumulateNextHeader(int protocolNumber, Buffer payload, Buffer extensionHeadersBuffer) throws IOException, FramingException {
        int headerExtensionLen;
        int nextHeaderProtocol;
        switch (protocolNumber) {
            case IPv6Packet.EXTENSION_HOP_BY_HOP:
            case IPv6Packet.EXTENSION_ROUTING:
            case IPv6Packet.EXTENSION_DESTINATION_OPTIONS:
                nextHeaderProtocol = payload.getByte(IPv6PacketImpl.FIXED_HEADER_LENGTH);
                headerExtensionLen = 8 + payload.getByte(IPv6PacketImpl.FIXED_HEADER_LENGTH + 1) * 8;
                break;
            case IPv6Packet.EXTENSION_FRAGMENT:
                nextHeaderProtocol = payload.getByte(IPv6PacketImpl.FIXED_HEADER_LENGTH);
                headerExtensionLen = 8;
                break;
            case IPv6Packet.EXTENSION_AH:
                nextHeaderProtocol = payload.getByte(IPv6PacketImpl.FIXED_HEADER_LENGTH);
                headerExtensionLen = 4 * (payload.getByte(IPv6PacketImpl.FIXED_HEADER_LENGTH + 1) + 2);
                break;
            case IPv6Packet.EXTENSION_ESP:
              // TODO figure out how length is even parsed...
            default:
                throw new FramingException(String.format("Unsupported IPv6 extension header: %d", protocolNumber), Protocol.IPv6);
        }
        Buffer tmp = payload.readBytes(headerExtensionLen);
        extensionHeadersBuffer.write(tmp.getArray());
        return nextHeaderProtocol;
    }

    @Override
    public boolean accept(final Buffer data) {
        // TODO Auto-generated method stub
        return false;
    }

}
