/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.node;

public class BlockRequest {
    public boolean bOnlyHeader = false;
    public final byte[] BlockHash;

    public BlockRequest(byte[] blockHash) {
        BlockHash = blockHash;
    }

    public BlockRequest(byte[] blockHash, boolean bOnlyHeader) {
        this.bOnlyHeader = bOnlyHeader;
        BlockHash = blockHash;
    }
}
