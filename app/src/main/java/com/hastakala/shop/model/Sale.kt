package com.hastakala.testshop.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sales",
    indices = [Index("userId")]
)
data class Sale(
    @PrimaryKey val id: String,       // UUID
    val userId: String,               // Firebase UID — scopes data per user
    val variantId: String,
    val productName: String,
    val variantLabel: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalAmount: Double,          // quantity * unitPrice
    val timestamp: Long               // epoch millis
)
