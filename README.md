# UISCoin

UISCoin is my attempt at making a cryptocurrency in Java. It is a SHA512 coin that uses a PoW algorithm based on the number of repeated characters in the set {U, I, S ...}  at the beginning of the block hash. UISCoin uses a [stack based scripting language](https://bradyrussell.github.io/UISCoin/com/bradyrussell/uiscoin/script/ScriptOperator.html) much like Bitcoin. Currently standard transactions in UISCoin use [P2PKH (pay to public key hash) scripts](https://github.com/bradyrussell/UISCoin/blob/fe920e8572bb361a8b3a035d3639f4988b70406e/src/com/bradyrussell/uiscoin/transaction/TransactionOutputBuilder.java#L21). All addresses take the form of `UIS(v)PUBKEYHASH(checksum)` where (v) is a version identifier and (checksum) is a check to ensure the address was not mistyped.

An example address is `UISxBraDYN4U4BN4bAKRLenuj1K37xr9zlhAfRFnKu-KLVf89uu3pBBSdBsF45cCAZwLhoK7Litdd4w8GxmkvHgBnFoqXis=`

Much like Bitcoin, we are using secp256k1 EC, but we sign messages with SHA512withECDSA.


# Differences from Bitcoin

While it was largely inspired by Bitcoin, there are some intentional differences between UISCoin and Bitcoin. 

- For one, we use SHA512 as a hash algorithm rather than SHA256 & RIPEMD160. This makes all hashes in UISCoin 64 bytes long rather than 32. We also only hash a single time.
- Instead of base58 encoding used by Bitcoin we use Base64 URL Encoded from Java's Base64.getUrlEncoder().
- The PoW difficulty is based on the [block hash beginning with N characters of a repeating series U, I, S, U, I, S ...](https://github.com/bradyrussell/UISCoin/blob/40b0327f5efbbfb06a320874aa1ac41bbeaa6344/src/com/bradyrussell/uiscoin/Hash.java#L58) rather than having a certain number of preceding zeroes (or being below a target value). It is limited at a minimum of 3 and a maximum of 63. An example of a valid block hash at difficulty 3 is:
`UISalWjd9-_4YTMOTVOQhthSwE4x-qUcXsZXT5zhjd3ic3bZaHj-Afjr1V0VpTZKZteYce-Zj5W-afEbsveh7w==`
- Java uses signed bytes, so I do not think the binary data produced can be used in languages with unsigned bytes without conversion.
- The block height is stored in the Index of the Coinbase transaction input rather than the script.
- The unlocking script signature message is the hash of the transaction output you are trying to spend. This allows you to combine multiple unrelated inputs into one transasction. (Not sure how this works in BTC)
- The [difficulty algorithm](https://github.com/bradyrussell/UISCoin/blob/40b0327f5efbbfb06a320874aa1ac41bbeaa6344/src/com/bradyrussell/uiscoin/block/BlockHeader.java#L130) is much simpler, basically if the last block was over five minutes ago, the difficulty is lastBlockDifficulty - 1. If it was before 5 minutes ago it is lastBlockDifficulty + 1. This is not as robust as Bitcoin's but works well enough.
- As alluded to in the previous bullet point, the target block time is five minutes rather than BTC's ten.

# API
JavaDoc is available here: https://bradyrussell.github.io/UISCoin/

For a demonstration of the API, here is how I created the Genesis Block.

        public static void CreateGenesisBlock() {
        BlockChain.Initialize(BlockChainStorageFile.class);
        BlockChain.get().open();

        UISCoinKeypair genesisCoins = UISCoinKeypair.Create();
        Wallet.SaveKeypairToFileWithPassword(Path.of("WALLET.uisw"), "PASSWORD", genesisCoins);

        byte[] address = UISCoinAddress.fromPublicKey((ECPublicKey) genesisCoins.Keys.getPublic());
        UISCoinAddress.DecodedAddress decodedAddress = UISCoinAddress.decodeAddress(address);

        long timestamp = Instant.now().getEpochSecond();
        
        BlockBuilder blockBuilder = new BlockBuilder().setVersion(1)
                .setTimestamp(timestamp)
                .setDifficultyTarget(3)
                .setBlockHeight(0)
                .setHashPreviousBlock(Hash.getSHA512Bytes("UISCoin 1.0 written by Brady Russell. Thanks to https://learnmeabitcoin.com/ and https://www.oreilly.com/library/view/mastering-bitcoin/9781491902639/ for the knowledge."))
                .addCoinbasePayToPublicKeyHash(decodedAddress.PublicKeyHash, "https://www.bbc.com/news - Trump and Melania test positive for coronavirus: The US president and Melania Trump were tested after his close aide was confirmed to have Covid-19. 8m minutes ago US & Canada") /* reference to Satoshi's The Times 03/Jan/2009 Chancellor on brink of second bailout for banks*/
                .CalculateMerkleRoot();
        
        System.out.println("Begin mining genesis block...");

        int i = Integer.MIN_VALUE;
        while (bIsRunning && !blockBuilder.get().Verify()) {
            blockBuilder.setNonce(i++);
        }

        Block finalBlock = blockBuilder.get();

        System.out.println(Util.Base64Encode(finalBlock.Header.getHash()));

        BlockChain.get().putBlock(finalBlock);
        node.BroadcastBlockToPeers(finalBlock);
        System.out.println("Genesis block broadcast!");
        BlockChain.get().close();
    }
    
    
# Scripting Language
The UISCoin scripting language is executed in bytecode format. Scripts can be converted back and forth between bytecode and script using ScriptBuilder.fromText() and ScriptBuilder.toText(). Scripts can also be created using the Builder pattern as seen below:
                
                byte[] a  = new ScriptBuilder(128)
                .op(ScriptOperator.DUP) // dup the public key
                .op(ScriptOperator.SHA512) // hash it
                .push(A) // push the address
                .op(ScriptOperator.LEN) // take its length
                .pushInt(4) // push 4
                .op(ScriptOperator.SWAP) // make length the top stack element, then 4
                .op(ScriptOperator.SUBTRACT) // do length - 4
                .op(ScriptOperator.LIMIT) // limit the address to length - 4 (remove checksum)
                .op(ScriptOperator.BYTESEQUAL) // equal to pubkey hash?
                .op(ScriptOperator.VERIFY)
                .op(ScriptOperator.VERIFYSIG)
                .get();

                byte[] b= new ScriptBuilder(128).fromText("dup sha512").push(A).fromText("len push 4 swap subtract limit bytesequal verify verifysig").get();
                assertTrue(Arrays.equals(a,b));// true
