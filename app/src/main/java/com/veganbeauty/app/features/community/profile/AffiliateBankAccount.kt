package com.veganbeauty.app.features.community.profile

data class AffiliateBankAccount(
    val id: Int,
    val bankName: String,
    val accountNumber: String,
    val accountHolder: String,
    val logo: String,
    var isDefault: Boolean
)
