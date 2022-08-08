/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.requests.eth

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import walletconnect.core.Failure

class EthTransactionTest {

    @Test
    fun validate_validModels() {
        FakeEthTransaction.AllValid.forEach { model ->
            assertEquals(model, model.validate())
        }
    }

    @Test
    fun validate_invalidContractCreation() {
        FakeEthTransaction.AllInvalids.forEach { model ->
            Assert.assertThrows(Failure::class.java) {
                model.validate()
            }
        }
    }

}