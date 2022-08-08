/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.core.requests.eth

import walletconnect.core.cryptography.toHex
import java.math.BigInteger

object FakeEthTransaction {

    val ValidEthTransfer = EthTransaction(
            from = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "0x621261D26847B423Df639848Fb53530025a008e8",
            data = "",
            chainId = 1.toHex(),

            gas = "0x9379",
            gasPrice = "0x2540be400",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "0x" + BigInteger("10000000000000000").toString(16),
            nonce = "0x1"
    )

    val ValidEthTransfer2 = EthTransaction(
            from = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "0x621261D26847B423Df639848Fb53530025a008e8",
            data = "",
            chainId = 1.toHex(),

            gas = "",
            gasPrice = "",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "0x" + BigInteger("10000000000000000").toString(16),
            nonce = ""
    )

    val ValidERC20Transfer = EthTransaction(
            from = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "0xcD0FC5078Befa4701C3692F4268641A468DEB8d5",
            data = "0xa9059cbb000000000000000000000000621261D26847B423Df639848Fb53530025a008e00000000000000000000000000000000000000000000000000000000000003e8",
            chainId = 97.toHex(),

            gas = "0x9379",
            gasPrice = "0x2540be400",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "",
            nonce = "0x1"
    )

    val ValidContractCreation = EthTransaction(
            from = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "",
            data = "60806040523480156200001157600080fd5b506040518060400160405280600d81526020017f5472697669616e20546f6b656e000000000000000000000000000000000000008152506040518060400160405280600681526020017f545249564941000000000000000000000000000000000000000000000000000081525081600390805190602001906200009692919062000414565b508060049080519060200190620000af92919062000414565b5050506000600560006101000a81548160ff021916908315150217905550620000ed620000e16200013360201b60201c565b6200013b60201b60201c565b6200012d33620001026200020160201b60201c565b600a6200011091906200065e565b633b9aca00620001219190620006af565b6200020a60201b60201c565b620008f5565b600033905090565b6000600560019054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905081600560016101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b60006003905090565b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614156200027d576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401620002749062000771565b60405180910390fd5b62000291600083836200038360201b60201c565b8060026000828254620002a5919062000793565b92505081905550806000808473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000828254620002fc919062000793565b925050819055508173ffffffffffffffffffffffffffffffffffffffff16600073ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef8360405162000363919062000801565b60405180910390a36200037f60008383620003f360201b60201c565b5050565b62000393620003f860201b60201c565b15620003d6576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401620003cd906200086e565b60405180910390fd5b620003ee8383836200040f60201b620009c61760201c565b505050565b505050565b6000600560009054906101000a900460ff16905090565b505050565b8280546200042290620008bf565b90600052602060002090601f01602090048101928262000446576000855562000492565b82601f106200046157805160ff191683800117855562000492565b8280016001018555821562000492579182015b828111156200049157825182559160200191906001019062000474565b5b509050620004a19190620004a5565b5090565b5b80821115620004c0576000816000905550600101620004a6565b5090565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b60008160011c9050919050565b6000808291508390505b600185111562000552578086048111156200052a5762000529620004c4565b5b60018516156200053a5780820291505b80810290506200054a85620004f3565b94506200050a565b94509492505050565b6000826200056d576001905062000640565b816200057d576000905062000640565b8160018114620005965760028114620005a157620005d7565b600191505062000640565b60ff841115620005b657620005b5620004c4565b5b8360020a915084821115620005d057620005cf620004c4565b5b5062000640565b5060208310610133831016604e8410600b8410161715620006115782820a9050838111156200060b576200060a620004c4565b5b62000640565b62000620848484600162000500565b925090508184048111156200063a5762000639620004c4565b5b81810290505b9392505050565b6000819050919050565b600060ff82169050919050565b60006200066b8262000647565b9150620006788362000651565b9250620006a77fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff84846200055b565b905092915050565b6000620006bc8262000647565b9150620006c98362000647565b9250817fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0483118215151615620007055762000704620004c4565b5b828202905092915050565b600082825260208201905092915050565b7f45524332303a206d696e7420746f20746865207a65726f206164647265737300600082015250565b600062000759601f8362000710565b9150620007668262000721565b602082019050919050565b600060208201905081810360008301526200078c816200074a565b9050919050565b6000620007a08262000647565b9150620007ad8362000647565b9250827fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff03821115620007e557620007e4620004c4565b5b828201905092915050565b620007fb8162000647565b82525050565b6000602082019050620008186000830184620007f0565b92915050565b7f5061757361626c653a2070617573656400000000000000000000000000000000600082015250565b60006200085660108362000710565b915062000863826200081e565b602082019050919050565b60006020820190508181036000830152620008898162000847565b9050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b60006002820490506001821680620008d857607f821691505b60208210811415620008ef57620008ee62000890565b5b50919050565b611e4e80620009056000396000f3fe608060405234801561001057600080fd5b50600436106101215760003560e01c806370a08231116100ad57806395d89b411161007157806395d89b41146102d2578063a457c2d7146102f0578063a9059cbb14610320578063dd62ed3e14610350578063f2fde38b1461038057610121565b806370a0823114610254578063715018a61461028457806379cc67901461028e5780638456cb59146102aa5780638da5cb5b146102b457610121565b8063313ce567116100f4578063313ce567146101c257806339509351146101e05780633f4ba83a1461021057806342966c681461021a5780635c975abb1461023657610121565b806306fdde0314610126578063095ea7b31461014457806318160ddd1461017457806323b872dd14610192575b600080fd5b61012e61039c565b60405161013b9190611383565b60405180910390f35b61015e6004803603810190610159919061143e565b61042e565b60405161016b9190611499565b60405180910390f35b61017c610451565b60405161018991906114c3565b60405180910390f35b6101ac60048036038101906101a791906114de565b61045b565b6040516101b99190611499565b60405180910390f35b6101ca61048a565b6040516101d7919061154d565b60405180910390f35b6101fa60048036038101906101f5919061143e565b610493565b6040516102079190611499565b60405180910390f35b6102186104ca565b005b610234600480360381019061022f9190611568565b610550565b005b61023e610564565b60405161024b9190611499565b60405180910390f35b61026e60048036038101906102699190611595565b61057b565b60405161027b91906114c3565b60405180910390f35b61028c6105c3565b005b6102a860048036038101906102a3919061143e565b61064b565b005b6102b261066b565b005b6102bc6106f1565b6040516102c991906115d1565b60405180910390f35b6102da61071b565b6040516102e79190611383565b60405180910390f35b61030a6004803603810190610305919061143e565b6107ad565b6040516103179190611499565b60405180910390f35b61033a6004803603810190610335919061143e565b610824565b6040516103479190611499565b60405180910390f35b61036a600480360381019061036591906115ec565b610847565b60405161037791906114c3565b60405180910390f35b61039a60048036038101906103959190611595565b6108ce565b005b6060600380546103ab9061165b565b80601f01602080910402602001604051908101604052809291908181526020018280546103d79061165b565b80156104245780601f106103f957610100808354040283529160200191610424565b820191906000526020600020905b81548152906001019060200180831161040757829003601f168201915b5050505050905090565b6000806104396109cb565b90506104468185856109d3565b600191505092915050565b6000600254905090565b6000806104666109cb565b9050610473858285610b9e565b61047e858585610c2a565b60019150509392505050565b60006003905090565b60008061049e6109cb565b90506104bf8185856104b08589610847565b6104ba91906116bc565b6109d3565b600191505092915050565b6104d26109cb565b73ffffffffffffffffffffffffffffffffffffffff166104f06106f1565b73ffffffffffffffffffffffffffffffffffffffff1614610546576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161053d9061175e565b60405180910390fd5b61054e610eab565b565b61056161055b6109cb565b82610f4d565b50565b6000600560009054906101000a900460ff16905090565b60008060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020549050919050565b6105cb6109cb565b73ffffffffffffffffffffffffffffffffffffffff166105e96106f1565b73ffffffffffffffffffffffffffffffffffffffff161461063f576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016106369061175e565b60405180910390fd5b6106496000611124565b565b61065d826106576109cb565b83610b9e565b6106678282610f4d565b5050565b6106736109cb565b73ffffffffffffffffffffffffffffffffffffffff166106916106f1565b73ffffffffffffffffffffffffffffffffffffffff16146106e7576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016106de9061175e565b60405180910390fd5b6106ef6111ea565b565b6000600560019054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905090565b60606004805461072a9061165b565b80601f01602080910402602001604051908101604052809291908181526020018280546107569061165b565b80156107a35780601f10610778576101008083540402835291602001916107a3565b820191906000526020600020905b81548152906001019060200180831161078657829003601f168201915b5050505050905090565b6000806107b86109cb565b905060006107c68286610847565b90508381101561080b576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610802906117f0565b60405180910390fd5b61081882868684036109d3565b60019250505092915050565b60008061082f6109cb565b905061083c818585610c2a565b600191505092915050565b6000600160008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008373ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054905092915050565b6108d66109cb565b73ffffffffffffffffffffffffffffffffffffffff166108f46106f1565b73ffffffffffffffffffffffffffffffffffffffff161461094a576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016109419061175e565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff1614156109ba576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016109b190611882565b60405180910390fd5b6109c381611124565b50565b505050565b600033905090565b600073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff161415610a43576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610a3a90611914565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff161415610ab3576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610aaa906119a6565b60405180910390fd5b80600160008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002060008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff168152602001908152602001600020819055508173ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167f8c5be1e5ebec7d5bd14f71427d1e84f3dd0314c0f7b2291e5b200ac8c7c3b92583604051610b9191906114c3565b60405180910390a3505050565b6000610baa8484610847565b90507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff8114610c245781811015610c16576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610c0d90611a12565b60405180910390fd5b610c2384848484036109d3565b5b50505050565b600073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff161415610c9a576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610c9190611aa4565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff161415610d0a576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610d0190611b36565b60405180910390fd5b610d1583838361128d565b60008060008573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002054905081811015610d9b576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610d9290611bc8565b60405180910390fd5b8181036000808673ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff16815260200190815260200160002081905550816000808573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000206000828254610e2e91906116bc565b925050819055508273ffffffffffffffffffffffffffffffffffffffff168473ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef84604051610e9291906114c3565b60405180910390a3610ea58484846112e5565b50505050565b610eb3610564565b610ef2576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610ee990611c34565b60405180910390fd5b6000600560006101000a81548160ff0219169083151502179055507f5db9ee0a495bf2e6ff9c91a7834c1ba4fdd244a5e8aa4e537bd38aeae4b073aa610f366109cb565b604051610f4391906115d1565b60405180910390a1565b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff161415610fbd576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401610fb490611cc6565b60405180910390fd5b610fc98260008361128d565b60008060008473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000205490508181101561104f576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161104690611d58565b60405180910390fd5b8181036000808573ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff1681526020019081526020016000208190555081600260008282546110a69190611d78565b92505081905550600073ffffffffffffffffffffffffffffffffffffffff168373ffffffffffffffffffffffffffffffffffffffff167fddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef8460405161110b91906114c3565b60405180910390a361111f836000846112e5565b505050565b6000600560019054906101000a900473ffffffffffffffffffffffffffffffffffffffff16905081600560016101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055508173ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff167f8be0079c531659141344cd1fd0a4f28419497f9722a3daafe3b4186f6b6457e060405160405180910390a35050565b6111f2610564565b15611232576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161122990611df8565b60405180910390fd5b6001600560006101000a81548160ff0219169083151502179055507f62e78cea01bee320cd4e420270b5ea74000d11b0c9f74754ebdbfc544b05a2586112766109cb565b60405161128391906115d1565b60405180910390a1565b611295610564565b156112d5576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016112cc90611df8565b60405180910390fd5b6112e08383836109c6565b505050565b505050565b600081519050919050565b600082825260208201905092915050565b60005b83811015611324578082015181840152602081019050611309565b83811115611333576000848401525b50505050565b6000601f19601f8301169050919050565b6000611355826112ea565b61135f81856112f5565b935061136f818560208601611306565b61137881611339565b840191505092915050565b6000602082019050818103600083015261139d818461134a565b905092915050565b600080fd5b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006113d5826113aa565b9050919050565b6113e5816113ca565b81146113f057600080fd5b50565b600081359050611402816113dc565b92915050565b6000819050919050565b61141b81611408565b811461142657600080fd5b50565b60008135905061143881611412565b92915050565b60008060408385031215611455576114546113a5565b5b6000611463858286016113f3565b925050602061147485828601611429565b9150509250929050565b60008115159050919050565b6114938161147e565b82525050565b60006020820190506114ae600083018461148a565b92915050565b6114bd81611408565b82525050565b60006020820190506114d860008301846114b4565b92915050565b6000806000606084860312156114f7576114f66113a5565b5b6000611505868287016113f3565b9350506020611516868287016113f3565b925050604061152786828701611429565b9150509250925092565b600060ff82169050919050565b61154781611531565b82525050565b6000602082019050611562600083018461153e565b92915050565b60006020828403121561157e5761157d6113a5565b5b600061158c84828501611429565b91505092915050565b6000602082840312156115ab576115aa6113a5565b5b60006115b9848285016113f3565b91505092915050565b6115cb816113ca565b82525050565b60006020820190506115e660008301846115c2565b92915050565b60008060408385031215611603576116026113a5565b5b6000611611858286016113f3565b9250506020611622858286016113f3565b9150509250929050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052602260045260246000fd5b6000600282049050600182168061167357607f821691505b602082108114156116875761168661162c565b5b50919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b60006116c782611408565b91506116d283611408565b9250827fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff038211156117075761170661168d565b5b828201905092915050565b7f4f776e61626c653a2063616c6c6572206973206e6f7420746865206f776e6572600082015250565b60006117486020836112f5565b915061175382611712565b602082019050919050565b600060208201905081810360008301526117778161173b565b9050919050565b7f45524332303a2064656372656173656420616c6c6f77616e63652062656c6f7760008201527f207a65726f000000000000000000000000000000000000000000000000000000602082015250565b60006117da6025836112f5565b91506117e58261177e565b604082019050919050565b60006020820190508181036000830152611809816117cd565b9050919050565b7f4f776e61626c653a206e6577206f776e657220697320746865207a65726f206160008201527f6464726573730000000000000000000000000000000000000000000000000000602082015250565b600061186c6026836112f5565b915061187782611810565b604082019050919050565b6000602082019050818103600083015261189b8161185f565b9050919050565b7f45524332303a20617070726f76652066726f6d20746865207a65726f2061646460008201527f7265737300000000000000000000000000000000000000000000000000000000602082015250565b60006118fe6024836112f5565b9150611909826118a2565b604082019050919050565b6000602082019050818103600083015261192d816118f1565b9050919050565b7f45524332303a20617070726f766520746f20746865207a65726f20616464726560008201527f7373000000000000000000000000000000000000000000000000000000000000602082015250565b60006119906022836112f5565b915061199b82611934565b604082019050919050565b600060208201905081810360008301526119bf81611983565b9050919050565b7f45524332303a20696e73756666696369656e7420616c6c6f77616e6365000000600082015250565b60006119fc601d836112f5565b9150611a07826119c6565b602082019050919050565b60006020820190508181036000830152611a2b816119ef565b9050919050565b7f45524332303a207472616e736665722066726f6d20746865207a65726f20616460008201527f6472657373000000000000000000000000000000000000000000000000000000602082015250565b6000611a8e6025836112f5565b9150611a9982611a32565b604082019050919050565b60006020820190508181036000830152611abd81611a81565b9050919050565b7f45524332303a207472616e7366657220746f20746865207a65726f206164647260008201527f6573730000000000000000000000000000000000000000000000000000000000602082015250565b6000611b206023836112f5565b9150611b2b82611ac4565b604082019050919050565b60006020820190508181036000830152611b4f81611b13565b9050919050565b7f45524332303a207472616e7366657220616d6f756e742065786365656473206260008201527f616c616e63650000000000000000000000000000000000000000000000000000602082015250565b6000611bb26026836112f5565b9150611bbd82611b56565b604082019050919050565b60006020820190508181036000830152611be181611ba5565b9050919050565b7f5061757361626c653a206e6f7420706175736564000000000000000000000000600082015250565b6000611c1e6014836112f5565b9150611c2982611be8565b602082019050919050565b60006020820190508181036000830152611c4d81611c11565b9050919050565b7f45524332303a206275726e2066726f6d20746865207a65726f2061646472657360008201527f7300000000000000000000000000000000000000000000000000000000000000602082015250565b6000611cb06021836112f5565b9150611cbb82611c54565b604082019050919050565b60006020820190508181036000830152611cdf81611ca3565b9050919050565b7f45524332303a206275726e20616d6f756e7420657863656564732062616c616e60008201527f6365000000000000000000000000000000000000000000000000000000000000602082015250565b6000611d426022836112f5565b9150611d4d82611ce6565b604082019050919050565b60006020820190508181036000830152611d7181611d35565b9050919050565b6000611d8382611408565b9150611d8e83611408565b925082821015611da157611da061168d565b5b828203905092915050565b7f5061757361626c653a2070617573656400000000000000000000000000000000600082015250565b6000611de26010836112f5565b9150611ded82611dac565b602082019050919050565b60006020820190508181036000830152611e1181611dd5565b905091905056fea264697066735822122094ee384bbbc06ff2b4fffb922b4e1dbd39c5c289c8c876aefb100f97ac656dbd64736f6c634300080b0033",
            chainId = 97.toHex(),

            gas = "0x9379",
            gasPrice = "0x2540be400",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "0x" + BigInteger("10000000000000000").toString(16),
            nonce = "0x0"
    )

    val InvalidContractCreation = EthTransaction(
            from = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "",
            data = "",
            chainId = 1.toHex(),

            gas = "0x9379",
            gasPrice = "0x2540be400",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "0x" + BigInteger("10000000000000000").toString(16),
            nonce = "0x1"
    )

    val InvalidFrom = EthTransaction(
            from = "0x2C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "0x621261D26847B423Df639848Fb53530025a008e8",
            data = "",
            chainId = 1.toHex(),

            gas = "0x9379",
            gasPrice = "0x2540be400",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "0x" + BigInteger("10000000000000000").toString(16),
            nonce = "0x1"
    )

    val InvalidTo = EthTransaction(
            from = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "0x21261D26847B423Df639848Fb53530025a008e8",
            data = "",
            chainId = 1.toHex(),

            gas = "0x9379",
            gasPrice = "0x2540be400",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "0x" + BigInteger("10000000000000000").toString(16),
            nonce = "0x1"
    )

    val InvalidValue = EthTransaction(
            from = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "0x621261D26847B423Df639848Fb53530025a008e8",
            data = "",
            chainId = 1.toHex(),

            gas = "0x9379",
            gasPrice = "0x2540be400",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "1",
            nonce = "0x1"
    )

    val InvalidGas = EthTransaction(
            from = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "0x621261D26847B423Df639848Fb53530025a008e8",
            data = "",
            chainId = 1.toHex(),

            gas = "21000",
            gasPrice = "0x2540be400",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "0x" + BigInteger("10000000000000000").toString(16),
            nonce = "0x1"
    )

    val InvalidGasPrice = EthTransaction(
            from = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "0x621261D26847B423Df639848Fb53530025a008e8",
            data = "",
            chainId = 1.toHex(),

            gas = "0x9379",
            gasPrice = "10",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "0x" + BigInteger("10000000000000000").toString(16),
            nonce = "0x1"
    )

    val InvalidNonce = EthTransaction(
            from = "0x02C4288c13cD24B9dC5b2bB637756e36a4bB2600",
            to = "0x621261D26847B423Df639848Fb53530025a008e8",
            data = "",
            chainId = 1.toHex(),

            gas = "0x9379",
            gasPrice = "0x2540be400",
            gasLimit = null,
            maxFeePerGas = null,
            maxPriorityFeePerGas = null,

            value = "0x" + BigInteger("10000000000000000").toString(16),
            nonce = "12"
    )

    val AllValid = listOf(
            ValidEthTransfer,
            ValidEthTransfer2,
            ValidERC20Transfer,
            ValidContractCreation
    )

    val AllInvalids = listOf(
            InvalidContractCreation,
            InvalidFrom,
            InvalidTo,
            InvalidGas,
            InvalidGasPrice,
            InvalidValue,
            InvalidNonce
    )

}