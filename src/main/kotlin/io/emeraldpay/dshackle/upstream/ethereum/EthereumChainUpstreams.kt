/**
 * Copyright (c) 2020 ETCDEV GmbH
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
package io.emeraldpay.dshackle.upstream.ethereum

import com.fasterxml.jackson.databind.ObjectMapper
import io.emeraldpay.dshackle.cache.Caches
import io.emeraldpay.dshackle.config.UpstreamsConfig
import io.emeraldpay.dshackle.upstream.*
import io.emeraldpay.grpc.Chain
import io.infinitape.etherjar.rpc.json.BlockJson
import io.infinitape.etherjar.rpc.json.TransactionJson
import io.infinitape.etherjar.rpc.json.TransactionRefJson
import org.slf4j.LoggerFactory
import org.springframework.context.Lifecycle
import java.lang.IllegalStateException
import java.time.Duration

class EthereumChainUpstreams(
        chain: Chain,
        val upstreams: MutableList<EthereumUpstream>,
        caches: Caches,
        objectMapper: ObjectMapper
) : ChainUpstreams<EthereumApi>(chain, upstreams as MutableList<Upstream<EthereumApi>>, caches, objectMapper) {

    companion object {
        private val log = LoggerFactory.getLogger(EthereumChainUpstreams::class.java)
    }

    private var head: EthereumHead? = null

    init {
        this.init()
    }

    override fun init() {
        if (upstreams.size > 0) {
            head = updateHead()
        }
        super.init()
    }

    override fun getHead(): EthereumHead {
        return head!!
    }

    override fun setHead(head: Head) {
        this.head = head as EthereumHead
    }

    override fun updateHead(): EthereumHead {
        head?.let {
            if (it is Lifecycle) {
                it.stop()
            }
        }
        lagObserver?.stop()
        lagObserver = null
        val head = if (upstreams.size == 1) {
            val upstream = upstreams.first()
            upstream.setLag(0)
            upstream.getHead()
        } else {
            val newHead = EthereumHeadMerge(upstreams.map { it.getHead() }).apply {
                this.start()
            }
            val lagObserver = EthereumHeadLagObserver(newHead, upstreams as Collection<Upstream<EthereumApi>>).apply {
                this.start()
            }
            this.lagObserver = lagObserver
            newHead
        }
        onHeadUpdated(head)
        return head
    }

    override fun getLabels(): Collection<UpstreamsConfig.Labels> {
        return upstreams.flatMap { it.getLabels() }
    }

    override fun printStatus() {
        var height: Long? = null
        try {
            height = getHead().getFlux().next().block(Duration.ofSeconds(1))?.height
        } catch (e: IllegalStateException) {
            //timout
        } catch (e: Exception) {
            log.warn("Head processing error: ${e.javaClass} ${e.message}")
        }
        val statuses = upstreams.map { it.getStatus() }
                .groupBy { it }
                .map { "${it.key.name}/${it.value.size}" }
                .joinToString(",")
        val lag = upstreams.map { it.getLag() }
                .joinToString(", ")

        log.info("State of ${chain.chainCode}: height=${height ?: '?'}, status=$statuses, lag=[$lag]")
    }

    @SuppressWarnings("unchecked")
    override fun <T : Upstream<TA>, TA : UpstreamApi> cast(selfType: Class<T>, upstreamType: Class<TA>): T {
        if (!selfType.isAssignableFrom(this.javaClass)) {
            throw ClassCastException("Cannot cast ${this.javaClass} to $selfType")
        }
        if (!upstreamType.isAssignableFrom(EthereumApi::class.java)) {
            throw ClassCastException("Cannot cast ${EthereumApi::class.java} to $upstreamType")
        }
        return this as T
    }

}