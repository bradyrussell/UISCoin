/* (C) Brady Russell 2021 */
package com.bradyrussell.uiscoin.node;

import com.bradyrussell.uiscoin.block.BlockHeader;

public class BlockHeaderResponse {
    public byte[] BlockHash;
    public BlockHeader blockHeader;

    public BlockHeaderResponse(byte[] blockHash, BlockHeader blockHeader) {
        BlockHash = blockHash;
        this.blockHeader = blockHeader;
    }
}
