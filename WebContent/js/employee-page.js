let new_movie_form = $("#new_movie_form");
let new_star_form = $("#new_star_form");

function handleAdd(resultData) {
    alert(resultData.message);
}

function onSubmitNewStar(event) {
    event.preventDefault();
    console.log("Star Form onSubmit");
    const form = event.target;

    const starName = form.elements['name'].value;
    const birthYear = form.elements['year'].value;
    console.log(starName, birthYear);

    $.ajax(
        `api/InsertStar?name=${starName}` + `${birthYear ? '&year='+birthYear : ''}`,
        {
            dataType: "json", // Setting return data type
            method: "POST", // Setting request method
            data: {
                starName: starName,
                birthYear: birthYear
            },
            success: (resultData) => handleAdd(resultData)
        }
    );
}

function onSubmitNewMovie(event) {
    event.preventDefault();
    console.log("Movie Form onSubmit");
    const form = event.target;

    const title = form.elements['title'].value;
    const year = form.elements['year'].value;
    const director = form.elements['director'].value;
    const star = form.elements['star'].value;
    const genre = form.elements['genre'].value;
    console.log(title, year, director, star, genre);
    $.ajax(
        `api/InsertMovie?title=${title}` + `&year=${year}` + `&director=${director}` + `&star=${star}` + `&genre=${genre}`,
        {
            dataType: "json", // Setting return data type
            method: "POST", // Setting request method
            data: {
                title: title,
                year: year,
                director: director,
                star: star,
                genre: genre
            },
            success: (resultData) => handleAdd(resultData)
        }
    );
}

function handleMetadata(resultData) {
    console.log(resultData);
    let metadata = $("#metadata-table-body");

    for (let i = 0; i < resultData.length; i++) {
        let table_name = resultData[i]["table"];
        let columnsArray = resultData[i]["columns"];

        let table_html = "";
        table_html += "<div>";
        table_html += "<h2 class='text-center'><strong> " + table_name + "</strong></h2>";
        table_html += "<table id='dashboard-table' class='table table-striped table-dark'>";
        table_html += "<thead>";
        table_html += "<tr class='text-center'> <th>Attribute</th> <th >Type</th> </tr>";
        table_html += "</thead>";
        table_html += "<tbody id='" + table_name + "-table-body'></tbody>";
        table_html += "</table></div>";

        metadata.append(table_html);

        let table_id = "#" + table_name + "-table-body";
        let dashboard_table = $(table_id);

        for (let j = 0; j < columnsArray.length; j++) {
            let column = columnsArray[j];
            let rowHTML = "";
            rowHTML += "<tr class='text-center'>";
            rowHTML += "<th>" + column.name + "</th>";
            rowHTML += "<th>" + column.type + "</th>";
            rowHTML += "</tr>";

            dashboard_table.append(rowHTML);
        }
    }
}


$.ajax(
    "api/Metadata", {
        dataType: "json", // Setting return data type
        method: "GET", // Setting request method
        data: new_star_form.serialize(),
        success: (resultData) => handleMetadata(resultData)
    }
);

new_movie_form.submit(onSubmitNewMovie);
new_star_form.submit(onSubmitNewStar);
