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

import org.bitcoinj.core.Block;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.VerificationException;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;

import java.math.BigInteger;

import static com.google.common.base.Preconditions.checkState;

/**
 * Network parameters for the regression test mode of dogecoin in which all blocks are trivially solvable.
 */
public class DogecoinRegTestParams extends DogecoinTestNet3Params {
    private static final BigInteger MAX_TARGET = new BigInteger("7fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", 16);

    public DogecoinRegTestParams() {
        super();
        // Difficulty adjustments are disabled for regtest.
        // By setting the block interval for difficulty adjustments to Integer.MAX_VALUE we make sure difficulty never changes.
        interval = Integer.MAX_VALUE;
        maxTarget = MAX_TARGET;
        subsidyDecreaseBlockCount = 150;
        port = 18236;
        id = ID_REGTEST;
        
    
        
        packetMagic = 0xfacec4da;
        addressHeader = 112;
        dumpedPrivateKeyHeader = 199;
    }

    @Override
    public boolean allowEmptyPeerChain() {
        return true;
    }

    private static Block genesis;

    @Override
    public Block getGenesisBlock() {
        synchronized (DogecoinRegTestParams.class) {
            if (genesis == null) {
                genesis = super.getGenesisBlock();
                genesis.setNonce(17);
                genesis.setDifficultyTarget(0x200fffffL);
                genesis.setTime(1524198906L);
                checkState(genesis.getVersion() == 1);
                checkState(genesis.getHashAsString().toLowerCase().equals("101336b273a9626bed8541eea8c716e5527ac5805681f41cf6e36d153ca7636f"));
                genesis.verifyHeader();
            }
            return genesis;
        }
    }

    private static DogecoinRegTestParams instance;

    public static synchronized DogecoinRegTestParams get() {
        if (instance == null) {
            instance = new DogecoinRegTestParams();
        }
        return instance;
    }

    @Override
    public String getPaymentProtocolId() {
        return PAYMENT_PROTOCOL_ID_REGTEST;
    }

    @Override
    /** the testnet rules don't work for regtest, where difficulty stays the same */
    public long calculateNewDifficultyTarget(StoredBlock storedPrev, Block nextBlock, BlockStore blockStore)
            throws VerificationException, BlockStoreException {
        final Block prev = storedPrev.getHeader();
        return prev.getDifficultyTarget();
    }

    @Override
    public boolean allowMinDifficultyBlocks() {
        return false;
    }

    @Override
    public boolean isTestNet() {
        return false;
    }
}
