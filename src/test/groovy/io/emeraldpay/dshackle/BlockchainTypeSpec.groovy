/**
 * Copyright (c) 2020 EmeraldPay, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.emeraldpay.dshackle

import io.emeraldpay.grpc.Chain
import spock.lang.Specification

class BlockchainTypeSpec extends Specification {

    def "Correct type for ethereum"() {
        expect:
        BlockchainType.fromBlockchain(chain) == BlockchainType.ETHEREUM
        where:
        chain << [Chain.ETHEREUM, Chain.ETHEREUM_CLASSIC, Chain.TESTNET_KOVAN, Chain.TESTNET_GOERLI, Chain.TESTNET_MORDEN, Chain.TESTNET_RINKEBY, Chain.TESTNET_ROPSTEN]
    }

    def "Correct type for bitcoin"() {
        expect:
        BlockchainType.fromBlockchain(chain) == BlockchainType.BITCOIN
        where:
        chain << [Chain.BITCOIN, Chain.TESTNET_BITCOIN]
    }

}
