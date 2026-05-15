package com.hastakala.testshop.util

import com.hastakala.testshop.model.Product
import com.hastakala.testshop.model.ProductVariant
import com.hastakala.testshop.model.Sale

// ---------------------------------------------------------------------------
// Sale serialization
// ---------------------------------------------------------------------------

fun Sale.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "variantId" to variantId,
    "productName" to productName,
    "variantLabel" to variantLabel,
    "quantity" to quantity,
    "unitPrice" to unitPrice,
    "totalAmount" to totalAmount,
    "timestamp" to timestamp
)

fun Map<String, Any?>.toSale(): Sale = Sale(
    id = this["id"] as String,
    userId = this["userId"] as? String ?: "",
    variantId = this["variantId"] as String,
    productName = this["productName"] as String,
    variantLabel = this["variantLabel"] as String,
    quantity = (this["quantity"] as Number).toInt(),
    unitPrice = (this["unitPrice"] as Number).toDouble(),
    totalAmount = (this["totalAmount"] as Number).toDouble(),
    timestamp = (this["timestamp"] as Number).toLong()
)

// ---------------------------------------------------------------------------
// Product serialization
// ---------------------------------------------------------------------------

fun Product.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "name" to name,
    "imageResId" to imageResId
)

fun Map<String, Any?>.toProduct(): Product = Product(
    id = this["id"] as String,
    userId = this["userId"] as? String ?: "",
    name = this["name"] as String,
    imageResId = (this["imageResId"] as? Number)?.toInt()
)

// ---------------------------------------------------------------------------
// ProductVariant serialization
// ---------------------------------------------------------------------------

fun ProductVariant.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "userId" to userId,
    "productId" to productId,
    "colorOrDesign" to colorOrDesign,
    "stock" to stock,
    "unitPrice" to unitPrice
)

fun Map<String, Any?>.toProductVariant(): ProductVariant = ProductVariant(
    id = this["id"] as String,
    userId = this["userId"] as? String ?: "",
    productId = this["productId"] as String,
    colorOrDesign = this["colorOrDesign"] as String,
    stock = (this["stock"] as Number).toInt(),
    unitPrice = (this["unitPrice"] as Number).toDouble()
)
