package com.example.data.model

data class CoinPack(
    val productId: String,
    val label: String,
    val fallbackPrice: String,
    val coins: Double
) {
    companion object {
        val packs = listOf(
            CoinPack("coin_pack_0_51", "0.51 coin", "$0.51", 0.51),
            CoinPack("coin_pack_1", "1 coin", "$1.00", 1.0),
            CoinPack("coin_pack_1_01", "1.01 coin", "$1.01", 1.01),
            CoinPack("coin_pack_2", "2 coin", "$2.00", 2.0),
            CoinPack("coin_pack_3", "3 coin", "$3.01", 3.0),
            CoinPack("coin_pack_4_01", "4.01 coin", "$4.01", 4.01),
            CoinPack("coin_pack_5_01", "5.01 coin", "$5.01", 5.01),
            CoinPack("coin_pack_10_01", "10.01 coin", "$10.01", 10.01),
            CoinPack("coin_pack_40_05", "40.05 coin", "$40.05", 40.05),
            CoinPack("coin_pack_50_01", "50.01 coin", "$50.01", 50.01)
        )
    }
}
