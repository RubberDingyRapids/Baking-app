package com.rubberdingyrapids.baking.core.data

/** Common baking and cooking ingredients used for inline autocomplete. */
object IngredientSuggestions {
    val common: List<String> = listOf(
        "all-purpose flour", "almond extract", "almonds", "apple", "apple cider vinegar", "apricots",
        "baking powder", "baking soda", "bananas", "basil", "bay leaves", "beef", "bell pepper",
        "bicarbonate of soda", "black pepper", "blueberries", "bread flour", "breadcrumbs",
        "brown sugar", "butter", "buttermilk", "cake flour", "caraway seeds", "cardamom", "carrots",
        "caster sugar", "cayenne pepper", "celery", "cheddar cheese", "cherries", "chicken",
        "chicken stock", "chili flakes", "chives", "chocolate", "chocolate chips", "cinnamon",
        "cloves", "cocoa powder", "coconut", "coconut milk", "coconut oil", "condensed milk",
        "coriander", "corn flour", "cornstarch", "cranberries", "cream", "cream cheese",
        "cream of tartar", "cumin", "currants", "dark chocolate", "dates", "demerara sugar",
        "digestive biscuits", "double cream", "dried yeast", "egg whites", "egg yolks", "eggs",
        "evaporated milk", "fresh yeast", "garlic", "gelatin", "ginger", "glucose syrup",
        "golden syrup", "granulated sugar", "greek yogurt", "ground almonds", "ground beef",
        "hazelnuts", "heavy cream", "honey", "icing sugar", "instant coffee", "instant yeast",
        "jam", "lard", "lemon", "lemon juice", "lemon zest", "lime", "maple syrup", "margarine",
        "marzipan", "mascarpone", "milk", "milk chocolate", "mixed spice", "molasses", "nutmeg",
        "oats", "olive oil", "onion", "orange", "orange juice", "orange zest", "oregano",
        "paprika", "parmesan", "parsley", "pasta", "peanut butter", "peanuts", "pears", "pecans",
        "pistachios", "plain flour", "poppy seeds", "potatoes", "powdered sugar", "puff pastry",
        "pumpkin", "raisins", "raspberries", "rice", "rice flour", "rosemary", "rye flour", "salt",
        "sea salt", "self-raising flour", "semolina", "sesame seeds", "shortening", "sour cream",
        "soy sauce", "sprinkles", "strawberries", "strong white flour", "sugar", "sultanas",
        "sunflower oil", "sweetened condensed milk", "thyme", "tomatoes", "treacle",
        "unsalted butter", "vanilla", "vanilla extract", "vegetable oil", "walnuts", "water",
        "white chocolate", "whole milk", "wholemeal flour", "yeast", "yogurt",
    ).sorted()

    /**
     * The best completion for what the user has typed so far, or null. Prefers
     * suggestions that start with the prefix, then ones with a word that does.
     * Returns the full suggestion so the UI can grey out the untyped remainder.
     */
    fun complete(prefix: String, extra: Collection<String> = emptyList()): String? {
        val typed = prefix.trimStart().lowercase()
        if (typed.isBlank()) return null
        val pool = (extra.map { it.lowercase() } + common).distinct()
        // The user has typed a complete known ingredient; don't push them further.
        if (typed in pool) return null
        return pool.firstOrNull { it.startsWith(typed) && it.length > typed.length }
    }

    /** Up to [limit] suggestions for a dropdown, best first. */
    fun suggest(prefix: String, extra: Collection<String> = emptyList(), limit: Int = 6): List<String> {
        val typed = prefix.trim().lowercase()
        if (typed.isBlank()) return emptyList()
        val pool = (extra.map { it.lowercase() } + common).distinct()
        val starts = pool.filter { it.startsWith(typed) }
        val wordStarts = pool.filter { it !in starts && it.split(' ', '-').any { w -> w.startsWith(typed) } }
        val contains = pool.filter { it !in starts && it !in wordStarts && it.contains(typed) }
        return (starts + wordStarts + contains).take(limit)
    }
}
