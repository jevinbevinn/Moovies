function handleCheckout(resultData) {
    console.log("handleMovieListResult: populating movie table from resultData");

    let spinnerLoader = jQuery("#spinnerLoader");
    let movieTableBodyElement = jQuery("#movie_table_body");

    const itemCounts = {};
    const movieIdToName = {};
    const itemPrices = {};

    // Count the occurrences of each movie ID
    resultData["previousItems"].forEach(item => {
        const movieId = item["id"];
        if (itemCounts[movieId]) {
            itemCounts[movieId]++;
        } else {
            itemCounts[movieId] = 1;
            movieIdToName[movieId] = item["name"];
            itemPrices[movieId] = parseInt(movieId.slice(-2));
        }
    });

    const rowsHTML = Object.keys(itemCounts).map(movieId => {
        const count = itemCounts[movieId];
        const movieName = movieIdToName[movieId];
        const generatedPrice = movieId.slice(-2);
        return `
            <tr>
            <th>${movieName}</th>
            <th>
                <button class="decrementCount" data-movieId="${movieId}" data-movieName="${movieName}">-</button>
                    <span class="count">${count}</span>
                <button class="incrementCount" data-movieId="${movieId}" data-movieName="${movieName}">+</button>
            </th>
            <th>
                <form id="cart" METHOD="post">
                    <input class="removeFromCartButton" type="submit" VALUE="Delete" data-movieId="${movieId}">
                </form>
            </th>
            <th><span class="generatedPrice">$${parseInt(generatedPrice)}</span></th>
            <th><span class="totalGeneratedPrice">$${generatedPrice * count}</span></th>
        </tr>
        `;
    });

    // Calculate the total
    const totalCost = Object.keys(itemCounts).reduce((total, movieId) => {
        const count = itemCounts[movieId];
        const itemPrice = itemPrices[movieId];
        return total + Number(itemPrice * count);
    }, 0);

    jQuery("p#totalCost").text("Total: $" + totalCost);

    jQuery(document).on("click", ".removeFromCartButton", function(event) {
        event.preventDefault(); // Prevent the form submission
        const movieId = jQuery(this).attr("data-movieId");
        jQuery.ajax({
            dataType: "json",
            method: "POST",
            url: "api/Cart" + `?remove=${movieId}`,
            success: (resultData) => {
                location.reload()
            },
            error: (error) => {
                alert(error);
            }
        });
    });

    jQuery(document).on("click", ".incrementCount", function(event) {
        event.preventDefault();
        const movieId = jQuery(this).attr("data-movieId");
        const movieName = jQuery(this).attr("data-movieName");
        jQuery.ajax({
            dataType: "json",
            method: "POST",
            url: "api/Cart" + `?movieId=${movieId}&title=${movieName}`,
            success: (resultData) => {
                // Update the count on the clicked button
                const countSpan = jQuery(this).siblings(".count");
                const newCount = parseInt(countSpan.text()) + 1;
                countSpan.text(newCount);

                // Update the generated price for this item
                const generatedPrice = movieId.slice(-2);
                const generatedPriceSpan = jQuery(this).closest("tr").find(".generatedPrice");
                generatedPriceSpan.text("$" + parseInt(generatedPrice));
                const totalGeneratedPriceSpan = jQuery(this).closest("tr").find(".totalGeneratedPrice");

                totalGeneratedPriceSpan.text(`$${generatedPrice * newCount}`)

                const itemCounts = {};
                resultData["previousItems"].forEach(item => {
                    const movieId = item["id"];
                    if (itemCounts[movieId]) {
                        itemCounts[movieId]++;
                    } else {
                        itemCounts[movieId] = 1;
                    }
                });
                const totalCost = Object.keys(itemCounts).reduce((total, movieId) => {
                    const count = itemCounts[movieId];
                    const itemPrice = itemPrices[movieId];
                    return total + Number(itemPrice * count);
                }, 0);
                jQuery("p#totalCost").text("Total: $" + totalCost);
            },
            error: (error) => {
                alert(error);
            }
        });
    });

    jQuery(document).on("click", ".decrementCount", function(event) {
        event.preventDefault();
        const movieId = jQuery(this).attr("data-movieId");
        jQuery.ajax({
            dataType: "json",
            method: "POST",
            url: `api/Cart?movieId=${movieId}&operation=decrement`,
            success: (resultData) => {
                // Update the count on the clicked button
                const countSpan = jQuery(this).siblings(".count");
                const newCount = parseInt(countSpan.text()) - 1;
                if (newCount === 0) {
                    jQuery.ajax({
                        dataType: "json",
                        method: "POST",
                        url: "api/Cart" + `?remove=${movieId}`,
                        success: (resultData) => {
                            location.reload()
                        },
                        error: (error) => {
                            alert(error);
                        }
                    });
                }
                countSpan.text(newCount);

                // Update the generated price for this item
                const generatedPrice = movieId.slice(-2);
                const generatedPriceSpan = jQuery(this).closest("tr").find(".generatedPrice");
                generatedPriceSpan.text("$" + parseInt(generatedPrice));
                const totalGeneratedPriceSpan = jQuery(this).closest("tr").find(".totalGeneratedPrice");

                totalGeneratedPriceSpan.text(`$${generatedPrice * newCount}`)

                const itemCounts = {};
                resultData["previousItems"].forEach(item => {
                    const movieId = item["id"];
                    if (itemCounts[movieId]) {
                        itemCounts[movieId]++;
                    } else {
                        itemCounts[movieId] = 1;
                    }
                });
                const totalCost = Object.keys(itemCounts).reduce((total, movieId) => {
                    const count = itemCounts[movieId];
                    const itemPrice = itemPrices[movieId];
                    return total + Number(itemPrice * count);
                }, 0);
                jQuery("p#totalCost").text("Total: $" + totalCost);

            },
            error: (error) => {
                alert(error);
            }
        });
    });

    // Hide spinner loader
    spinnerLoader.addClass('d-none');

    // Update the table body with the generated rows
    movieTableBodyElement.html(rowsHTML.join(''));
}

let purchaseButton = jQuery("#purchaseButton");

jQuery(document).on("click", "#purchaseButton", function(event) {
    event.preventDefault();
    console.log("Clicked purchase");
    window.location.replace("payment.html");
});

jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/Cart",
    success: (resultData) => {
        if (resultData["previousItems"].length === 0) {
            purchaseButton.addClass('disabled');
        } else {
            purchaseButton.removeClass('disabled');
        }
        handleCheckout(resultData)
    }
});
