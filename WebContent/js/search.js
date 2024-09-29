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
function handleSearch(resultData) {
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

    let paginationBodyElement = jQuery("#pagination_body");
    const pageNumber = $("#current-page a.page-link").text();
    if (pageNumber === "1") {
        // Disable Previous button
        paginationBodyElement.find("#prev-page").addClass("disabled");

    }
    if (resultData.length < parseInt($("#moviePerPage").val())){
        // Disable next button
        paginationBodyElement.find("#next-page").addClass("disabled");
    }

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
    $("#textLoader").addClass('d-none');

    // Join the array of HTML row strings and update the table body
    movieTableBodyElement.html(rowsHTML.join(''));
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Get params from URL
let title = getParameterByName("title");
let year = getParameterByName("year");
let director = getParameterByName("director");
let star = getParameterByName("star");
let genre = getParameterByName("genre");
let sortingOptions = getParameterByName("sorting");
let limit = getParameterByName("limit");
let page = getParameterByName("page");

$("#current-page a.page-link").text(page)

let searchUrl = "?";

if (title) { searchUrl += `title=${title}&`;}
if (year) { searchUrl += `year=${year}&`;}
if (director) { searchUrl += `director=${director}&`;}
if (star) { searchUrl += `star=${star}&`;}
if (genre) { searchUrl += `genre=${genre}&`;}

let completedSearchUrl = searchUrl;

if (sortingOptions) {
    sortingQ = `sorting=${sortingOptions}&`;
    searchUrl += sortingQ;
}

if (limit) {
    limitQ = `limit=${limit}&`;
    searchUrl += limitQ;
}

if (page) {
    pageQ = `page=${page}&`;
    searchUrl += pageQ;
}
if (title === null && year === null && director === null && star === null && genre === null && sortingOptions === null && limit === null && page === null) {
    let toUrl = sessionStorage.getItem("searchUrl");
    if (toUrl !== null) {
        document.location.href = toUrl;
    }
    $("#spinnerLoader").hide();
    $("#textLoader").show();
    sortingOptions = "titleRatingAA";
    limit = "10"
    page = "1";

} else {
    sessionStorage.setItem("searchUrl", completedSearchUrl + sortingQ + limitQ + pageQ);
}
searchUrl = searchUrl.slice(0, -1);

let numMoviesPerPage = $("#moviePerPage");
let sortDropdown = $("#sortingDropdown");

numMoviesPerPage.val(limit);
sortDropdown.val(sortingOptions);

// Event Listener for MoviesPerPage
numMoviesPerPage.on("change", function() {
    // This function will be executed whenever the movies per page changes
    $("#spinnerLoader").show();
    $("#textLoader").hide();
    const selectedValue = $(this).val();
    const newUrl = completedSearchUrl + sortingQ + `limit=${selectedValue}&` + 'page=1';
    document.location.href = newUrl;
});

// Event Listener for sorting
sortDropdown.on("change", function() {
    // This function will be executed whenever the movies per page changes
    $("#spinnerLoader").show();
    $("#textLoader").hide();
    const selectedValue = $(this).val();
    const newUrl = completedSearchUrl + `sorting=${selectedValue}&` + limitQ + 'page=1';
    document.location.href = newUrl;
});

let paginationBodyElement = jQuery("#pagination_body");

function updatePageAndReload(newPage) {
    document.location.href = completedSearchUrl + sortingQ + limitQ + `page=${newPage}`;
}

// Event listener for Previous button
paginationBodyElement.find("#prev-page a.page-link").on("click", function(e) {
    e.preventDefault();
    const newPage = parseInt(page) - 1;
    if (newPage >= 1) {
        updatePageAndReload(newPage);
    }
});

// Event listener for the "Next" button
paginationBodyElement.find("#next-page a.page-link").on("click", function(e) {
    e.preventDefault(); // Prevent the default link behavior
    const newPage = parseInt(page) + 1;
    updatePageAndReload(newPage);
});


jQuery.ajax({
    dataType: "json",
    method: "GET",
    url: "api/Search" + searchUrl,
    success: (resultData) => {
        handleSearch(resultData);
        // Check if the number of results is less than the limit per page
        if (resultData.length < parseInt(numMoviesPerPage.val())) {
            // Disable the "Next" button
            paginationBodyElement.find("#next-page").addClass("disabled");
        }
    }
})
