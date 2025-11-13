package org.example.db.mysql;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Helper methods for MySQL protocol implementation
 */
class MySQLProtocolHelper {
    
    // Read MySQL packet from input stream
    static byte[] readPacket(InputStream in) throws Exception {
        // Read 4-byte header
        byte[] header = new byte[4];
        readFully(in, header);
        
        int length = (header[0] & 0xFF) | ((header[1] & 0xFF) << 8) | ((header[2] & 0xFF) << 16);
        byte sequenceId = header[3];
        
        // Read payload
        byte[] payload = new byte[length];
        readFully(in, payload);
        
        return payload;
    }
    
    // Write MySQL packet to output stream
    static void writePacket(OutputStream out, byte[] payload, int sequenceId) throws Exception {
        int length = payload.length;
        
        // Write header
        out.write(length & 0xFF);
        out.write((length >> 8) & 0xFF);
        out.write((length >> 16) & 0xFF);
        out.write(sequenceId & 0xFF);
        
        // Write payload
        out.write(payload);
        out.flush();
    }
    
    // Read exact number of bytes
    static void readFully(InputStream in, byte[] buffer) throws Exception {
        int offset = 0;
        while (offset < buffer.length) {
            int read = in.read(buffer, offset, buffer.length - offset);
            if (read == -1) throw new RuntimeException("Unexpected EOF");
            offset += read;
        }
    }
    
    // Compute SHA1 hash
    static byte[] sha1(byte[] data) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return md.digest(data);
    }
    
    // XOR two byte arrays
    static byte[] xor(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }
    
    // Get length-encoded integer (simplified for small values)
    static int getLengthEncodedInt(byte[] data, int offset) {
        int first = data[offset] & 0xFF;
        if (first < 0xFB) {
            return first;
        }
        // For simplicity, not handling larger encodings
        return 0;
    }
    
    // Write null-terminated string
    static void writeNullTerminatedString(java.io.ByteArrayOutputStream out, String s) {
        out.write(s.getBytes(StandardCharsets.UTF_8), 0, s.length());
        out.write(0);
    }
    
    // Write length-encoded string
    static void writeLengthEncodedString(java.io.ByteArrayOutputStream out, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        out.write(bytes.length);
        out.write(bytes, 0, bytes.length);
    }
}
