/**
 * MIT License
 * Copyright (c) 2022 Jemshit Iskenderov
 */
package walletconnect.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import walletconnect.sample.databinding.AllSeeingActivityBinding

class OneActivity : AppCompatActivity() {

    private lateinit var binding: AllSeeingActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AllSeeingActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

}
