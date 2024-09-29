/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs three steps:
 *      1. Get parameter from request URL so it know which id to look for
 *      2. Use jQuery to talk to backend API to get the json data.
 *      3. Populate the data to correct html elements.
 */


/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */

function handleResult(resultData) {
    console.log("handleResult: populating star info from resultData");

    let starNameElement = jQuery("#single_star_name");
    let starDobElement = jQuery("#single_star_dob");

    let dateOfBirth = resultData[0]["star_dob"];
    if (dateOfBirth === null || dateOfBirth === undefined) {
        dateOfBirth = "N/A";
    }

    console.log("handleResult: populating movie table from resultData");

    // Find the empty table body by id "star_table_body"
    let movieTableBodyElement = jQuery("#star_table_body");

    const rowsHTML = resultData.map(movie => `
        <tr>
            <th><a href='single-movie.html?id=${movie["movie_id"]}'>${movie["movie_title"]}</a></th>
            <th>${movie["movie_year"]}</th>
            <th>${movie["movie_director"]}</th>
            <th>
                <form class="addToCartForm">
                    <button class="addToCartButton" type="submit">Add</button>
                </form>
            </th>
        </tr>
    `);

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

    // Set the HTML content of the table body
    movieTableBodyElement.html(rowsHTML.join(''));

    // Set the Star Name and DOB text
    starNameElement.text(resultData[0]["star_name"]);
    starDobElement.text("(" + dateOfBirth + ")");

    // Update page title
    document.title = resultData[0]["star_name"];
}

/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
let starId = getParameterByName('id');

// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/single-star?id=" + starId, // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the SingleStarServlet
});