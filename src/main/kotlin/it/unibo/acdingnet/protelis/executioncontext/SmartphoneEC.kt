package it.unibo.acdingnet.protelis.executioncontext

import it.unibo.acdingnet.protelis.node.SmartphoneNode
import org.protelis.vm.ExecutionEnvironment
import org.protelis.vm.NetworkManager
import org.protelis.vm.impl.SimpleExecutionEnvironment

class SmartphoneEC(
    private val smartphoneNode: SmartphoneNode,
    netmgr: NetworkManager,
    randomSeed: Int = 1,
    execEnvironment: ExecutionEnvironment = SimpleExecutionEnvironment()
) : PositionedExecutionContext(smartphoneNode.deviceUID, smartphoneNode.position,
    netmgr, randomSeed, execEnvironment) {

    override fun instance(): SmartphoneEC = this
}
