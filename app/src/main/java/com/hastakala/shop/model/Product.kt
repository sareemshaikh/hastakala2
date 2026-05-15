package com.hastakala.testshop.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index("userId")]
)
data class Product(
    @PrimaryKey val id: String,       // UUID
    val userId: String,               // Firebase UID — scopes data per user
    val name: String,
    val imageResId: Int? = null       // drawable resource for icon
)
