/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.libdohj.params;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;

import org.bitcoinj.core.AltcoinBlock;
import org.bitcoinj.core.Block;
import org.bitcoinj.core.Coin;
import static org.bitcoinj.core.Coin.COIN;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.script.Script;
import org.bitcoinj.script.ScriptOpCodes;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;
import org.bitcoinj.utils.MonetaryFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Utils;
import org.libdohj.core.AltcoinSerializer;
import org.libdohj.core.AuxPoWNetworkParameters;

/**
 * Common parameters for Dogecoin networks.
 */
public abstract class AbstractDogecoinParams extends NetworkParameters implements AuxPoWNetworkParameters {
    /** Standard format for the DOGE denomination. */
    public static final MonetaryFormat DOGE;
    /** Standard format for the mDOGE denomination. */
    public static final MonetaryFormat MDOGE;
    /** Standard format for the Koinu denomination. */
    public static final MonetaryFormat KOINU;

    public static final int DIGISHIELD_BLOCK_HEIGHT = 100; // Block height to use Digishield from
    public static final int AUXPOW_CHAIN_ID = 0x2000; // 98
    public static final int DOGE_TARGET_TIMESPAN = 7 * 24 * 60 * 60;  // 4 hours per difficulty cycle, on average.
    public static final int DOGE_TARGET_TIMESPAN_NEW = 5 * 60;  // 60s per difficulty cycle, on average. Kicks in after block 145k.
    public static final int DOGE_TARGET_SPACING = 5 * 60;  // 1 minute per block.
    public static final int DOGE_INTERVAL = DOGE_TARGET_TIMESPAN / DOGE_TARGET_SPACING;
    public static final int DOGE_INTERVAL_NEW = DOGE_TARGET_TIMESPAN_NEW / DOGE_TARGET_SPACING;

    /** Currency code for base 1 Dogecoin. */
    public static final String CODE_DOGE = "URX";
    /** Currency code for base 1/1,000 Dogecoin. */
    public static final String CODE_MDOGE = "mURX";
    /** Currency code for base 1/100,000,000 Dogecoin. */
    public static final String CODE_KOINU = "geigers";

    
    private static final int BLOCK_MIN_VERSION_AUXPOW =  0x20000000;

    //private static final int BLOCK_MIN_VERSION_AUXPOW =  0x00620004;
    private static final int BLOCK_VERSION_FLAG_AUXPOW = 0x00000100;

    
    // or 0x00620004
    //  private static final int BLOCK_MIN_VERSION_AUXPOW = 0x00620002;
  //  private static final int BLOCK_VERSION_FLAG_AUXPOW = 0x00000001;
    
    static {
        DOGE = MonetaryFormat.BTC.noCode()
            .code(0, CODE_DOGE)
            .code(3, CODE_MDOGE)
            .code(7, CODE_KOINU);
        MDOGE = DOGE.shift(3).minDecimals(2).optionalDecimals(2);
        KOINU = DOGE.shift(7).minDecimals(0).optionalDecimals(2);
    }

    /** The string returned by getId() for the main, production network where people trade things. */
    public static final String PAYMENT_PROTOCOL_ID_MAINNET = "org.dogecoin.production";
    /** The string returned by getId() for the testnet. */
    public static final String PAYMENT_PROTOCOL_ID_TESTNET = "org.dogecoin.test";
    
        public static final String PAYMENT_PROTOCOL_ID_REGTEST = "org.dogecoin.regtest";


    protected final int newInterval;
    protected final int newTargetTimespan;
    
    
    protected final int diffChangeTarget;

    protected Logger log = LoggerFactory.getLogger(AbstractDogecoinParams.class);

    /*
    has to be greater than proto 70003 project originally uses 70003 here check the actual desktop wallet coin version.h
        for more insight about the packet version and subversion of the actual protocol called from version.h of the radiocoind 
            radiocoin-qt source
            https://github.com/c4pt000/radiocoin/blob/master/src/version.h
    */
    
        public static final int DOGECOIN_PROTOCOL_VERSION_AUXPOW = 70003;
    public static final int DOGECOIN_PROTOCOL_VERSION_CURRENT = 70003;
    //5B recheck this soon of actual big number of supply of coin
   private static final Coin BASE_SUBSIDY   = COIN.multiply(10000000);
    private static final Coin STABLE_SUBSIDY = COIN.multiply(10000);

    public AbstractDogecoinParams(final int setDiffChangeTarget) {
        super();
        genesisBlock = createGenesis(this);
        interval = DOGE_INTERVAL;
        newInterval = DOGE_INTERVAL_NEW;
        targetTimespan = DOGE_TARGET_TIMESPAN;
        newTargetTimespan = DOGE_TARGET_TIMESPAN_NEW;
        maxTarget = Utils.decodeCompactBits(0x1e7fffffL);
        diffChangeTarget = setDiffChangeTarget;
// https://github.com/c4pt000/radiocoin/blob/c2c4c5f9dbf38b985b43077d506c161a3e91a8df/src/chainparams.cpp#L180
        
        packetMagic = 0xf9d9cdc3;

        
         bip32HeaderP2PKHpub = 0x0488b21e; //The 4 byte header that serializes in base58 to "dgub".
        bip32HeaderP2PKHpriv =  0x0488ade4; 
        
       // bip32HeaderP2PKHpub = 0x0488C42E; //The 4 byte header that serializes in base58 to "xpub". (?)
        //bip32HeaderP2PKHpriv = 0x0488E1F4; //The 4 byte header that serializes in base58 to "xprv" (?)
    }

    private static AltcoinBlock createGenesis(NetworkParameters params) {
        AltcoinBlock genesisBlock = new AltcoinBlock(params, Block.BLOCK_VERSION_GENESIS);
        Transaction t = new Transaction(params);
        try {
            byte[] bytes = Utils.HEX.decode
                // could be byte flipped here differently f0ff0f1e0 "1e0f
                      //  1e7f  ->        ffff7f1e
                       // ffff
                
                       // 1e0f    -> 0f1e
                      //  fff0
                
                       // f0 ff 0f 1e
                       // ff ff f7 
                //fff0" , 10 for 16 bytes by psz. starts with 04 for byte padding 
                 // 04f0ff 0f1e   010410526164696f436f696e2077616c6c6574
              //  ("04 f0ff0f1e 010410 526164696f436f696e2077616c6c6574");
                //psz phrase UraniumX 20/April/2018 Radioactive Blockchain Discovered in Deep Space
                // phrase is 140 characters / 2 is 70 bytes , 70 in hex is 46 as (70) bytes
    //            ("04 ffff7f1e 0104 46 5572616e69756d582032302f417072696c2f3230313820526164696f61637469766520426c6f636b636861696e20446973636f766572656420696e2044656570205370616365");  

            ("04ffff7f1e0104465572616e69756d582032302f417072696c2f3230313820526164696f61637469766520426c6f636b636861696e20446973636f766572656420696e2044656570205370616365");  
            t.addInput(new TransactionInput(params, t, bytes));
            ByteArrayOutputStream scriptPubKeyBytes = new ByteArrayOutputStream();
            Script.writeBytes(scriptPubKeyBytes, Utils.HEX.decode
                    ("04584ca4018adf8b6b26edb1d2ac869a3ad91f8b92c1d93a89bd7c2142a5f42332a3896625ca5acb660004d4a07d73ec46b456e494cca331e7f3cfb12e0af68538"));
            scriptPubKeyBytes.write(ScriptOpCodes.OP_CHECKSIG);
            t.addOutput(new TransactionOutput(params, t, COIN.multiply(1), scriptPubKeyBytes.toByteArray()));
        } catch (Exception e) {
            // Cannot happen.
            throw new RuntimeException(e);
        }
        genesisBlock.addTransaction(t);
        return genesisBlock;
    }

    @Override
    public Coin getBlockSubsidy(final int height) {
        if (height < DIGISHIELD_BLOCK_HEIGHT) {
            // Up until the Digishield hard fork, subsidy was based on the
            // previous block hash. Rather than actually recalculating that, we
            // simply use the maximum possible here, and let checkpoints enforce
            // that new blocks with different values can't be mined
            return BASE_SUBSIDY.shiftRight(height / getSubsidyDecreaseBlockCount()).multiply(2);
        } else if (height < 600000) {
            return BASE_SUBSIDY.shiftRight(height / getSubsidyDecreaseBlockCount());
        } else {
            return STABLE_SUBSIDY;
        }
    }

    /** How many blocks pass between difficulty adjustment periods. After new diff algo. */
    public int getNewInterval() {
        return newInterval;
    }

    /**
     * How much time in seconds is supposed to pass between "interval" blocks. If the actual elapsed time is
     * significantly different from this value, the network difficulty formula will produce a different value.
     * Dogecoin after block 145k uses 60 seconds.
     */
    public int getNewTargetTimespan() {
        return newTargetTimespan;
    }

    public MonetaryFormat getMonetaryFormat() {
        return DOGE;
    }

    @Override
    public Coin getMaxMoney() {
        // TODO: Change to be Doge compatible
        return MAX_MONEY;
    }

    @Override
    public Coin getMinNonDustOutput() {
        return Coin.COIN;
    }

    //part of the prefix here such as radiocoin:<ADDRESS> when generating a QR code for URI import
    //paid and free "block" of scanning the QR deposit address from the free android QR for Google Play console app of Radiocoin Wallet
    @Override
    public String getUriScheme() {
     //   return "THE FREE VERSION DOES NOT SUPPORT ANDROID TO ANDROID (ONLY ANDROID TO DESKTOP) UPGRADE TO PAID";
        
            return "";

    }

    @Override
    public boolean hasMaxMoney() {
        return false;
    }

    /** Dogecoin: Normally minimum difficulty blocks can only occur in between
     * retarget blocks. However, once we introduce Digishield every block is
     * a retarget, so we need to handle minimum difficulty on all blocks.
     */
    private boolean allowDigishieldMinDifficultyForBlock(final StoredBlock pindexLast, final Block pblock) {
        // check if the chain allows minimum difficulty blocks
        if (!this.allowMinDifficultyBlocks())
            return false;

        // check if the chain allows minimum difficulty blocks on recalc blocks
        if (pindexLast.getHeight() < 157500)
            return false;

        // Allow for a minimum block time if the elapsed time > 2*nTargetSpacing
        return (pblock.getTimeSeconds() > pindexLast.getHeader().getTimeSeconds() + this.getTargetSpacing(pindexLast.getHeight() + 1) * 2);
    }

    @Override
    public void checkDifficultyTransitions(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
        throws VerificationException, BlockStoreException {
        try {
            final long newTargetCompact = calculateNewDifficultyTarget(storedPrev, nextBlock, blockStore);
            final long receivedTargetCompact = nextBlock.getDifficultyTarget();

            if (newTargetCompact != receivedTargetCompact)
System.out.println("higher target value than expected");
       //         throw new VerificationException("Network provided difficulty bits do not match what was calculated: " +
         //               newTargetCompact + " vs " + receivedTargetCompact);
        } catch (CheckpointEncounteredException ex) {
            // Just have to take it on trust then
        }
    }

    /**
     * Get the difficulty target expected for the next block. This includes all
     * the weird cases for Dogecoin such as testnet blocks which can be maximum
     * difficulty if the block interval is high enough.
     *
     * @throws CheckpointEncounteredException if a checkpoint is encountered while
     * calculating difficulty target, and therefore no conclusive answer can
     * be provided.
     */
    public long calculateNewDifficultyTarget(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
        throws VerificationException, BlockStoreException, CheckpointEncounteredException {
        // Dogecoin: Special rules for minimum difficulty blocks with Digishield
        if (allowDigishieldMinDifficultyForBlock(storedPrev, nextBlock))
        {
            // Special difficulty rule for testnet:
            // If the new block's timestamp is more than 2* nTargetSpacing minutes
            // then allow mining of a min-difficulty block.
            return Utils.encodeCompactBits(this.getMaxTarget());
        }

        final Block prev = storedPrev.getHeader();
        final int previousHeight = storedPrev.getHeight();
        final boolean digishieldAlgorithm = previousHeight + 1 >= this.getDigishieldBlockHeight();
        final int retargetInterval = digishieldAlgorithm
            ? this.getNewInterval()
            : this.getInterval();

        // Is this supposed to be a difficulty transition point?
        if ((storedPrev.getHeight() + 1) % retargetInterval != 0) {
            if (this.allowMinDifficultyBlocks()) {
                // Special difficulty rule for testnet:
                // If the new block's timestamp is more than 2 minutes
                // then allow mining of a min-difficulty block.
                if (nextBlock.getTimeSeconds() > prev.getTimeSeconds() + getTargetSpacing(previousHeight + 1) * 2) {
                    return Utils.encodeCompactBits(maxTarget);
                } else {
                    // Return the last non-special-min-difficulty-rules-block
                    StoredBlock cursor = storedPrev;

                    while (cursor.getHeight() % retargetInterval != 0
                            && cursor.getHeader().getDifficultyTarget() == Utils.encodeCompactBits(this.getMaxTarget())) {
                        StoredBlock prevCursor = cursor.getPrev(blockStore);
                        if (prevCursor == null) {
                            break;
                        }
                        cursor = prevCursor;
                    }

                    return cursor.getHeader().getDifficultyTarget();
                }
            }

            // No ... so check the difficulty didn't actually change.
            return prev.getDifficultyTarget();
        }

        // We need to find a block far back in the chain. It's OK that this is expensive because it only occurs every
        // two weeks after the initial block chain download.
        StoredBlock cursor = storedPrev;
        int goBack = retargetInterval - 1;
        if (cursor.getHeight()+1 != retargetInterval)
            goBack = retargetInterval;

        for (int i = 0; i < goBack; i++) {
            if (cursor == null) {
                // This should never happen. If it does, it means we are following an incorrect or busted chain.
                throw new VerificationException(
                        "Difficulty transition point but we did not find a way back to the genesis block.");
            }
            cursor = blockStore.get(cursor.getHeader().getPrevBlockHash());
        }

        //We used checkpoints...
        if (cursor == null) {
            log.debug("Difficulty transition: Hit checkpoint!");
            throw new CheckpointEncounteredException();
        }

        Block blockIntervalAgo = cursor.getHeader();
        return this.calculateNewDifficultyTargetInner(previousHeight, prev.getTimeSeconds(),
            prev.getDifficultyTarget(), blockIntervalAgo.getTimeSeconds(),
            nextBlock.getDifficultyTarget());
    }

    /**
     * Calculate the difficulty target expected for the next block after a normal
     * recalculation interval. Does not handle special cases such as testnet blocks
     * being setting the target to maximum for blocks after a long interval.
     *
     * @param previousHeight height of the block immediately before the retarget.
     * @param prev the block immediately before the retarget block.
     * @param nextBlock the block the retarget happens at.
     * @param blockIntervalAgo The last retarget block.
     * @return New difficulty target as compact bytes.
     */
    protected long calculateNewDifficultyTargetInner(int previousHeight, final Block prev,
            final Block nextBlock, final Block blockIntervalAgo) {
        return this.calculateNewDifficultyTargetInner(previousHeight, prev.getTimeSeconds(),
            prev.getDifficultyTarget(), blockIntervalAgo.getTimeSeconds(),
            nextBlock.getDifficultyTarget());
    }

    /**
     * Calculate the difficulty target expected for the next block after a normal
     * recalculation interval.
     * 
     * @param previousHeight Height of the block immediately previous to the one we're calculating difficulty of.
     * @param previousBlockTime Time of the block immediately previous to the one we're calculating difficulty of.
     * @param lastDifficultyTarget Compact difficulty target of the last retarget block.
     * @param lastRetargetTime Time of the last difficulty retarget.
     * @param nextDifficultyTarget The expected difficulty target of the next
     * block, used for determining precision of the result.
     * @return New difficulty target as compact bytes.
     */
    protected long calculateNewDifficultyTargetInner(int previousHeight, long previousBlockTime,
        final long lastDifficultyTarget, final long lastRetargetTime,
        final long nextDifficultyTarget) {
        final int height = previousHeight + 1;
        final boolean digishieldAlgorithm = height >= this.getDigishieldBlockHeight();
        final int retargetTimespan = digishieldAlgorithm
            ? this.getNewTargetTimespan()
            : this.getTargetTimespan();
        int actualTime = (int) (previousBlockTime - lastRetargetTime);
        final int minTimespan;
        final int maxTimespan;

        // Limit the adjustment step.
        if (digishieldAlgorithm)
        {
            // Round towards zero to match the C++ implementation.
            if (actualTime < retargetTimespan) {
                actualTime = (int)Math.ceil(retargetTimespan + (actualTime - retargetTimespan) / 8.0);
            } else {
                actualTime = (int)Math.floor(retargetTimespan + (actualTime - retargetTimespan) / 8.0);
            }
            minTimespan = retargetTimespan - (retargetTimespan / 4);
            maxTimespan = retargetTimespan + (retargetTimespan / 2);
        }
        else if (height > 10000)
        {
            minTimespan = retargetTimespan / 4;
            maxTimespan = retargetTimespan * 4;
        }
        else if (height > 5000)
        {
            minTimespan = retargetTimespan / 8;
            maxTimespan = retargetTimespan * 4;
        }
        else
        {
            minTimespan = retargetTimespan / 16;
            maxTimespan = retargetTimespan * 4;
        }
        actualTime = Math.min(maxTimespan, Math.max(minTimespan, actualTime));

        BigInteger newTarget = Utils.decodeCompactBits(lastDifficultyTarget);
        newTarget = newTarget.multiply(BigInteger.valueOf(actualTime));
        newTarget = newTarget.divide(BigInteger.valueOf(retargetTimespan));

        if (newTarget.compareTo(this.getMaxTarget()) > 0) {
            log.info("Difficulty hit proof of work limit: {}", newTarget.toString(16));
            newTarget = this.getMaxTarget();
        }

        int accuracyBytes = (int) (nextDifficultyTarget >>> 24) - 3;

        // The calculated difficulty is to a higher precision than received, so reduce here.
        BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
        newTarget = newTarget.and(mask);
        return Utils.encodeCompactBits(newTarget);
    }

    /**
     * Get the block height from which the Digishield difficulty calculation
     * algorithm is used.
     */
    public int getDigishieldBlockHeight() {
        return DIGISHIELD_BLOCK_HEIGHT;
    }

    @Override
    public int getChainID() {
        return AUXPOW_CHAIN_ID;
    }

    /**
     * Whether this network has special rules to enable minimum difficulty blocks
     * after a long interval between two blocks (i.e. testnet).
     */
    public abstract boolean allowMinDifficultyBlocks();

    /**
     * Get the hash to use for a block.
     */
    @Override
    public BigInteger getBlockDifficulty(Block block) {
        return ((AltcoinBlock) block).getScryptHash().toBigInteger();
    }

    @Override
    public AltcoinSerializer getSerializer(boolean parseRetain) {
        return new AltcoinSerializer(this, parseRetain);
    }

    @Override
    public int getProtocolVersionNum(final ProtocolVersion version) {
        switch (version) {
            case PONG:
           case BLOOM_FILTER:
                return version.getBitcoinProtocolVersion();
            case CURRENT:
                return DOGECOIN_PROTOCOL_VERSION_CURRENT;
            case MINIMUM:
            default:
                return DOGECOIN_PROTOCOL_VERSION_AUXPOW;
        }
    }

    @Override
    public boolean isAuxPoWBlockVersion(long version) {
        return version >= BLOCK_MIN_VERSION_AUXPOW
            && (version & BLOCK_VERSION_FLAG_AUXPOW) > 0;
    }

    /**
     * Get the target time between individual blocks. Dogecoin uses this in its
     * difficulty calculations, but most coins don't.
     *
     * @param height the block height to calculate at.
     * @return the target spacing in seconds.
     */
    protected int getTargetSpacing(int height) {
        final boolean digishieldAlgorithm = height >= this.getDigishieldBlockHeight();
        final int retargetInterval = digishieldAlgorithm
            ? this.getNewInterval()
            : this.getInterval();
        final int retargetTimespan = digishieldAlgorithm
            ? this.getNewTargetTimespan()
            : this.getTargetTimespan();
        return retargetTimespan / retargetInterval;
    }

    private static class CheckpointEncounteredException extends Exception {

        private CheckpointEncounteredException() {
        }
    }
}
