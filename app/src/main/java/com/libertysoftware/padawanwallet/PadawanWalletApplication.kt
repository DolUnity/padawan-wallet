/*
 * Copyright 2020 thunderbiscuit and contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the ./LICENSE file.
 */

package com.libertysoftware.padawanwallet

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import org.bitcoindevkit.bdkjni.Lib
import org.bitcoindevkit.bdkjni.Types.ExtendedKeys
import org.bitcoindevkit.bdkjni.Types.Network
import org.bitcoindevkit.bdkjni.Types.WalletConstructor
import org.bitcoindevkit.bdkjni.Types.WalletPtr
import timber.log.Timber

class PadawanWalletApplication : Application() {

    private lateinit var lib: Lib
    private lateinit var walletPtr: WalletPtr
    private lateinit var name: String
    private lateinit var network: String
    private lateinit var path: String
    private lateinit var descriptor: String
    private lateinit var changeDescriptor: String
    private lateinit var electrumURL: String

    override fun onCreate() {
        super.onCreate()

        // logs
        Timber.plant(Timber.DebugTree())

        // bitcoindevkit
        Lib.load()
        this.lib = Lib()
        setDefaults()
    }

    override fun onTerminate(): Unit {
        super.onTerminate()
        lib.destructor(walletPtr)
    }

    private fun setDefaults(): Unit {
        this.name = "Padawan Testnet 0"
        this.network = "testnet"
        this.path = applicationContext.filesDir.toString()
        this.electrumURL = "tcp://testnet.aranguren.org:51001"
    }

    fun initialize(
        name: String,
        path: String,
        descriptor: String,
        changeDescriptor: String,
        electrumURL: String,
        electrumProxy: String?,
    ): Unit {
        this.name = name
        this.path = path
        this.descriptor = descriptor
        this.changeDescriptor = changeDescriptor
        this.electrumURL = electrumURL

        walletPtr = lib.constructor(
            WalletConstructor(
                name = name,
                network = Network.testnet,
                path = path,
                descriptor = descriptor,
                change_descriptor = changeDescriptor,
                electrum_url = electrumURL,
                electrum_proxy = electrumProxy,
            )
        )
    }

    fun createWallet(descriptor: String, changeDescriptor: String): Unit {
        this.setDefaults()
        this.initialize(
            name = this.name,
            path = this.path,
            descriptor = descriptor,
            changeDescriptor = changeDescriptor,
            electrumURL = this.electrumURL,
            electrumProxy = null,
        )
        this.saveWallet()
    }

    // save wallet parameters so that the wallet can be reloaded upon starting the app
    private fun saveWallet(): Unit {
        val editor: SharedPreferences.Editor = getSharedPreferences("current_wallet", Context.MODE_PRIVATE).edit()
        editor.putBoolean("initialized", true)
        editor.putString("name", this.name)
        editor.putString("network", this.network)
        editor.putString("path", this.path)
        editor.putString("descriptor", this.descriptor)
        editor.putString("changeDescriptor", this.changeDescriptor)
        editor.putString("electrumURL", this.electrumURL)
        editor.apply()
    }


    public fun generateExtendedKey(mnemonicWordCount: Int): ExtendedKeys {
        Timber.i("Extended keys generated")
        return lib.generate_extended_key(Network.testnet, mnemonicWordCount)
    }

    public fun createDescriptor(keys: ExtendedKeys): String {
        return ("wpkh(" + keys.ext_priv_key + "/0/*)")
    }

    public fun createChangeDescriptor(keys: ExtendedKeys): String {
        return ("wpkh(" + keys.ext_priv_key + "/1/*)")
    }

    public fun getNewAddress(): String {
        return lib.get_new_address(walletPtr)
    }
}
