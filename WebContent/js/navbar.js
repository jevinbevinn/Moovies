function handleLookup(query, doneCallback) {
    if (query < 3) { return;}

    console.log("looking up suggestions");

    let cachedResult = localStorage.getItem(query)
    if (cachedResult) {
        console.log("using cached query")
        handleLookupAjaxSuccess(cachedResult, query, doneCallback)
        return;
    }

    jQuery.ajax({
        method: "GET",
        url: "api/Autocomplete" + `?query=${query}`,
        success: (resultData) => {
            handleLookupAjaxSuccess(resultData, query, doneCallback);
        }
    });
}

function handleLookupAjaxSuccess(data, query, doneCallback) {
    console.log("lookup ajax successful")

    // parse the string into JSON
    let jsonData = JSON.parse(data);
    console.log(jsonData)

    let cachedResult = localStorage.getItem(query)
    if (!cachedResult) {
        console.log("caching data")
        localStorage.setItem(query, data)
    }

    // call the callback function provided by the autocomplete library
    // add "{suggestions: jsonData}" to satisfy the library response format according to
    //   the "Response Format" section in documentation
    doneCallback( { suggestions: jsonData } );
}

function handleSelectSuggestion(suggestion) {
    window.location.href = `single-movie.html?id=${suggestion.data['movieId']}`
}

function createNavBar() {
    document.getElementById("navBar").innerHTML =  `
        <div class="container-fluid">
            <a class="navbar-brand" href="index.html">Home</a>
            <ul class="navbar-nav gap-3">
                <li class="nav-item"><a href="top20.html">Top 20</a></li>
                <li class="nav-item"><a href="search.html">Results</a></li>
                <li class="nav-item"><a href="checkout.html">Checkout</a></li>
            </ul>
            <form class="d-flex gap-2" id="searchForm">
                <input class="autocomplete-searchbox form-control" type="search" placeholder="Title" aria-label="title" id="autocomplete">
                <input class="form-control" type="search" placeholder="Year" aria-label="year" id="yearInput">
                <input class="form-control" type="search" placeholder="Director" aria-label="director" id="directorInput">
                <input class="form-control" type="search" placeholder="Star" aria-label="Star" id="starInput">
                <button class="btn btn-warning my-2 my-sm-0" type="submit" id="searchButton">Search</button>
            </form>
        </div>
    `;

    // Set the active link in the navigation bar
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.navbar-nav a');
    navLinks.forEach((link) => {
        const linkPath = link.getAttribute('href');
        if (currentPath.includes(linkPath)) {
            link.classList.add('active');
        } else {
            link.classList.remove('active');
        }
    });

    const titleInput = document.getElementById("autocomplete");
    const yearInput = document.getElementById("yearInput");
    const directorInput = document.getElementById("directorInput");
    const starInput = document.getElementById("starInput");
    const searchButton = document.getElementById("searchButton");
    searchButton.classList.add("disabled");

    function checkInputs() {
        if (titleInput.value || yearInput.value || directorInput.value || starInput.value) {
            searchButton.classList.remove("disabled");
        } else {
            searchButton.classList.add("disabled");
        }
    }

    const urlParams = new URLSearchParams(window.location.search);

    titleInput.value = urlParams.get("title") || '';
    yearInput.value = urlParams.get("year") || '';
    directorInput.value = urlParams.get("director") || '';
    starInput.value = urlParams.get("star") || '';

    titleInput.addEventListener("input", checkInputs);
    yearInput.addEventListener("input", checkInputs);
    directorInput.addEventListener("input", checkInputs);
    starInput.addEventListener("input", checkInputs);

    document.getElementById("searchForm").addEventListener("submit", function(event) {
        event.preventDefault();

        // Get the values from the input fields
        const title = titleInput.value;
        const year = yearInput.value;
        const director = directorInput.value;
        const star = starInput.value;

        let searchUrl = "search.html?";
        if (title) { searchUrl += `title=${title}&`;}
        if (year) { searchUrl += `year=${year}&`;}
        if (director) { searchUrl += `director=${director}&`;}
        if (star) { searchUrl += `star=${star}&`;}
        searchUrl += "sorting=titleRatingAA&limit=10&page=1&"
        searchUrl = searchUrl.slice(0, -1);

        console.log(searchUrl)
        window.location.href = searchUrl;
    });
}

createNavBar();

$('#autocomplete').autocomplete({
    lookup: function (query, doneCallback) {
        handleLookup(query, doneCallback)
    },
    onSelect: function (suggestion) {
        handleSelectSuggestion(suggestion);
    },
    deferRequestBy: 300,
    minChars: 3,
});