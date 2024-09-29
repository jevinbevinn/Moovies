function setBrowse(resultData) {
    console.log("setBrowse: populating buttons for browse");

    const genreList = resultData.map(function (genres) {
        const genre = genres["genre"];
        return `<a href='search.html?genre=${genre}&sorting=titleRatingAA&limit=10&page=1' class="btn btn-warning col-2">${genre}</a> `
    });

    jQuery("#list_of_genres").html(`<div class="flex-wrap d-flex justify-content-center" role="group" aria-label="Genre buttons">${genreList.join('')}</div>`);

    const charList = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '*'];

    const titleList = charList.map(function (titles) {
        return `<a href='search.html?title=${titles}&sorting=titleRatingAA&limit=10&page=1' class="btn btn-warning col-1">${titles}</a> `
    });

    jQuery("#alphanum").html(`<div class="flex-wrap d-flex justify-content-center" role="group" aria-label="Title buttons">${titleList.join('')}</div>`);
}

jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/GetGenres", // Setting request URL
    success: (resultData) => setBrowse(resultData) // Setting callback function to handle data returned successfully
});
