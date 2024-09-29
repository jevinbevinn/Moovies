let orderTableBodyElement = jQuery("#order_table_body");

function handleConf(resultData) {
    console.log("handleConfResult: populating confirmation table from resultData");
    const itemCounts = {};
    const movieIdToName = {};
    const saleIDS = {}

    // Count the occurrences of each movie ID
    resultData["previousItems"].forEach(item => {
        const movieId = item["id"];
        if (itemCounts[movieId]) {
            itemCounts[movieId]++;
        } else {
            itemCounts[movieId] = 1;
            movieIdToName[movieId] = item["name"];
            saleIDS[movieId] = item["saleID"]
        }
    });

    const rowsHTML = Object.keys(itemCounts).map(movieId => {
        const count = itemCounts[movieId];
        const movieName = movieIdToName[movieId];
        const saleID = saleIDS[movieId]
        return `
            <tr>
                <th>${saleID}</th>
                <th>${movieName}</th>
                <th>${movieId}</th>
                <th>${count}</th>
            </tr>
            `;
    });

    orderTableBodyElement.html(rowsHTML.join(''));
}

$.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/Confirmation",
    success: (resultData) => handleConf(resultData)
});