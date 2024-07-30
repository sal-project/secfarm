const get_apis = {
    "courselist": "/lecture/api/get-courseinfo",
    "packagelist": "/lecture/api/get-pkginfo",
    "contentlist": "/lecture/api/get-contents"
};

const app_url = {
    "course": "/lecture",
    "package": /^\/lecture\/c\/[a-zA-Z0-9-]+/,
    "contents": /^\/lecture\/c\/[a-zA-Z0-9-]+\/[a-zA-Z0-9-]+/
};

const mode = function(){
    let path = location.pathname;
    let splited_path = path.split("/");

    if(path === "/lecture") {
        return {"mode": "courselist",
                "id": []};
    }else if(path.match(app_url["contents"])) {
        return {"mode": "contentlist",
                "id": [splited_path[3],
                       splited_path[4]]};
    }else if(path.match(app_url["package"])){
        return {"mode": "packagelist",
                "id": [splited_path[3]]};
    }else{
        return {"mode": null,
                "id": []};
    }
}();

function page_back(){
    if(mode["mode"] === "courselist"){
        location.href = "/";
    }else if(mode["mode"] === "packagelist"){
        location.href = "/lecture";
    }else{
        location.href = location.pathname.split('/').slice(0,-1).join('/');
    }
}

function append_course(divelem, linkelem, info){
    let title = document.createElement("p");
    title.textContent = info["title"];
    title.setAttribute("class", "sw_title title is-4 has-text-link");

    let id = document.createElement("p");
    id.textContent = info["id"];
    id.setAttribute("class", "sw_info subtitle is-6 has-text-link");

    let license = document.createElement("p");
    license.textContent = "License: " + info["license"];
    license.setAttribute("class", "sw_info");

    let description = document.createElement("div");
    description.textContent = info["description"];
    description.setAttribute("class", "sw_text content");

    divelem.appendChild(title);
    divelem.appendChild(id);
    divelem.appendChild(license);
    divelem.appendChild(description);
    
    linkelem.setAttribute('href', "/lecture/c/" + info["id"]);
}

function append_package(divelem, linkelem, info){
    let title = document.createElement("p");
    title.textContent = info["title"];
    title.setAttribute("class", "sw_title title is-4 has-text-link");

    let id = document.createElement("p");
    id.textContent = info["id"];
    id.setAttribute("class", "sw_info subtitle is-6 has-text-link");

    let description = document.createElement("div");
    description.textContent = info["description"];
    description.setAttribute("class", "sw_text content");

    divelem.appendChild(title);
    divelem.appendChild(id);
    divelem.appendChild(description);

    linkelem.setAttribute('href', "/lecture/c/" + info["parent"] + "/" + info["id"]);
}

function append_content(divelem, linkelem, info) {
    console.log(info);
    
    let title = document.createElement("p");
    if(info["nickname"] === "undefined" || typeof info["nickname"] !== "string" || info["nickname"].length === 0){
        title.textContent = info["content"];
    }else{
        title.textContent = info["nickname"];
    }
    title.setAttribute("class", "sw_title title is-4 has-text-link");

    let ctype = document.createElement("p");
    ctype.textContent = "Type: " + info["type"];
    ctype.setAttribute("class", "sw_tag subtitle is-6 has-text-link");

    //let full_id = document.createElement("p");
    //full_id.textContent = info["parent"][0] + "/" + info["parent"][1];
    //full_id.setAttribute("class", "sw_info subtitle is-6");

    divelem.appendChild(title);
    divelem.appendChild(ctype);
    //divelem.appendChild(full_id);

    let extra_info = document.createElement("div");
    extra_info.setAttribute("class", "sw_extra content");

    if(info["type"] === "test" && info["extra"] !== null){
        let score = info["extra"]["score"];
        let maxscore = info["extra"]["scoremax"];
        extra_info.textContent = `受講済み: ${score}/${maxscore} (正答率 ${Math.round(score/maxscore*100)}%)`;
    }else if(info["type"] === "test" && info["extra"] === null) {
        extra_info.textContent = "未受講";
    }else if(info["type"] === "doc" && info["extra"] !== null && info["extra"]["progress"] === 100){
        extra_info.textContent = "受講済み";
    }else if(info["type"] === "doc" && info["extra"] !== null && info["extra"]["progress"] < 100){
        extra_info.textContent = "未完了";
    }else{
        extra_info.textContent = "";
    }
    divelem.appendChild(extra_info);

    linkelem.setAttribute("href",
                          "/lecture/c/"
                          + info["parent"][0]
                          + "/"
                          + info["parent"][1]
                          + "/"
                          + info["content"]);
}

function append_lists(listdiv, info, appendfn){
    let new_elem = document.createElement("div");
    let link_elem = document.createElement("a");
    new_elem.setAttribute("class", "selectwindow cell");
    
    let card_elem = document.createElement("div");
    let content_elem = document.createElement("div");
    card_elem.setAttribute("class", "card");
    content_elem.setAttribute("class", "sw_contents card-content");

    appendfn(content_elem, link_elem, info);

    card_elem.appendChild(content_elem);
    link_elem.appendChild(card_elem);
    new_elem.appendChild(link_elem);
    
    return listdiv.appendChild(new_elem);
}

function append_infos(infos){
    let listtop = document.getElementById("listtop");
    if(mode["mode"] === "courselist") {
        infos.forEach((elem) => append_lists(listtop,
                                             elem,
                                             append_course));
    }else if(mode["mode"] === "packagelist") {
        let packages = infos["packages"].map((package) => {
            return Object.assign(package, {"parent": infos["course_id"]});
        });
        packages.forEach((elem) => append_lists(listtop,
                                                elem,
                                                append_package));
    }else if(mode["mode"] == "contentlist") {
        let contents = infos["contents"].map((content) => {
            return Object.assign(content, {"parent": [infos["course_id"],
                                                     infos["package_id"]]});
        });
        contents.forEach((elem) => append_lists(listtop,
                                                elem,
                                                append_content));
    }
}

function make_get_req(modeinfo){
    if(modeinfo["mode"] === "courselist"){
        return get_apis[modeinfo["mode"]];
    }else if(modeinfo["mode"] === "contentlist"){
        return get_apis[modeinfo["mode"]]
            + "?cid="
            + modeinfo["id"][0]
            + "&pid="
            + modeinfo["id"][1];
    }else if(modeinfo["mode"] === "packagelist"){
        return get_apis[modeinfo["mode"]]
            + "?pid="
            + modeinfo["id"][0];
    }else{
        return null;
    }
}

window.addEventListener('DOMContentLoaded', function() {
    let access = make_get_req(mode);
    if(access !== null){
        fetch(access)
            .then((res) => {return res.json();})
            .then((result) => {
                console.log(result);
                append_infos(result);
            })
            .catch((e) => {
                alert("Failed to fetch information.");
                console.log(e);
            });
    }else{
        alert("Invalid mode. Please reload.");
    }
});
