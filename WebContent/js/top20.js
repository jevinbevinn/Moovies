/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs two steps:
 *      1. Use jQuery to talk to backend API to get the json data.
 *      2. Populate the data to correct html elements.
 */

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleMovieListResult(resultData) {
    console.log("handleMovieListResult: populating movie table from resultData");

    let spinnerLoader = jQuery("#spinnerLoader");
    // Find the empty table body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#movie_table_body");

    const rowsHTML = resultData.map(movie => {
        const starsArray = movie["star_names"].split(', ');
        const starIdsArray = movie["star_ids"].split(', ');
        const starsHTML = starsArray.map((star, index) => {
            return `<a href='single-star.html?id=${starIdsArray[index]}'>${star}</a>`;
        }).join(', ');

        return `
            <tr>
                <th><a href='single-movie.html?id=${movie["movieId"]}'>${movie["title"]}</a></th>
                <th>${movie["year"]}</th>
                <th>${movie["director"]}</th>
                <th>${movie["genres"]}</th>
                <th>${starsHTML}</th>
                <th>${movie["rating"]}</th>
                <th>
                    <form class="addToCartForm">
                        <button class="addToCartButton" type="submit">Add</button>
                    </form>
                </th>
            </tr>
        `;
    });


    $(document).on("click", ".addToCartButton", function(event) {
        event.preventDefault();

        // Find the closest ancestor tr element containing the button
        const closestTr = this.closest('tr');

        // Find the anchor element within the closest tr
        const anchor = closestTr.querySelector('th a');

        // Extract the movieId from the href attribute
        const movieId = anchor.getAttribute('href').split('=')[1];
        const movieName = anchor.textContent;

        jQuery.ajax({
                dataType: "json",
                method: "POST",
                url: "api/Cart" + `?movieId=${movieId}&title=${movieName}`,
                success: (resultData) => {
                    alert("Successfully added \"" + movieName + "\" to your cart!")
                },
                error: (error) => {
                    alert("An error occured. \"" + movieName + "\" is not added to your cart.")

                }
            }
        );
    });

    // Hide spinner loader
    spinnerLoader.addClass('d-none');

    // Join the array of HTML row strings and update the table body
    movieTableBodyElement.html(rowsHTML.join(''));
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Makes the HTTP GET request and registers on success callback function handleStarResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/MovieList", // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleMovieListResult(resultData) // Setting callback function to handle data returned successfully by the StarsServlet
});