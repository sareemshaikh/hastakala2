package com.hastakala.testshop.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "product_variants",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId"), Index("userId")]
)
data class ProductVariant(
    @PrimaryKey val id: String,       // UUID
    val userId: String,               // Firebase UID — scopes data per user
    val productId: String,
    val colorOrDesign: String,        // e.g. "Red", "Striped"
    val stock: Int,
    val unitPrice: Double
)
