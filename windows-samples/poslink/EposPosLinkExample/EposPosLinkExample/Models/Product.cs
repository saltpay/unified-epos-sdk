using System.Collections.Generic;

namespace EposPosLinkExample.Models;

public record Product(int Id, string Name, decimal Price)
{
    public static List<Product> GetProducts() =>
    [
        new(1, "Apple", 0.99m),
        new(2, "Banana", 0.59m),
        new(3, "Orange", 0.79m),
        new(4, "Grapes", 2.49m),
        new(5, "Mango", 1.99m),
        new(6, "Peach", 1.29m),
        new(7, "Lemon", 1.29m),
        new(8, "Lime", 1.29m),
        new(9, "Strawberry", 1.29m),
        new(10, "Watermelon", 1.29m),
    ];
}
