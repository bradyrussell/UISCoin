# UISCoin

UISCoin is my attempt at making a cryptocurrency in Java. It is a SHA512 coin that uses a PoW algorithm based on the number of repeated characters in the set {U, I, S ...}  at the beginning of the block hash. UISCoin uses a stack based scripting language much like Bitcoin. Currently standard transactions in UISCoin use P2PKH (pay to public key hash) scripts. All addresses take the form of UIS(v)PUBKEYHASH(checksum) where (v) is a version identifier and (checksum) is a check to ensure the address was not mistyped.

Much like Bitcoin, we are using secp256k1 EC, but we sign messages with SHA512withECDSA.


# Differences from Bitcoin

While it was largely inspired by Bitcoin, there are some intentional differences between UISCoin and Bitcoin. 

- For one, we use SHA512 as a hash algorithm rather than SHA256 & RIPEMD160. This makes all hashes in UISCoin 64 bytes long rather than 32. 
- Instead of base58 encoding used by Bitcoin we use Base64 URL Encoded from Java's Base64.getUrlEncoder().
- The PoW difficulty is based on the block hash beginning with N characters of a repeating series U, I, S, U, I, S ... rather than having a certain number of preceding zeroes (or being below a target value). It is limited at a minimum of 3 and a maximum of 63. An example of a valid block hash at difficulty 3 is:
`UISalWjd9-_4YTMOTVOQhthSwE4x-qUcXsZXT5zhjd3ic3bZaHj-Afjr1V0VpTZKZteYce-Zj5W-afEbsveh7w==`
- Java uses signed bytes, so I do not think the binary data produced can be used in languages with unsigned bytes without conversion.
- The block height is stored in the Index of the Coinbase transaction input rather than the script.
- The unlocking script signature message is the hash of the transaction output you are trying to spend. This allows you to combine multiple unrelated inputs into one transasction. (Not sure how this works in BTC)
- The difficulty algorithm is much simpler, basically if the last block was over five minutes ago, the difficulty is lastBlockDifficulty - 1. If it was before 5 minutes ago it is lastBlockDifficulty + 1. This is not as robust as Bitcoin's but works well enough.
- As alluded to in the previous bullet point, the target block time is five minutes rather than BTC's ten.
