const contents_url = "/lecture/api/get-contents";

function get_ids(){
    const path = location.pathname.split("/");
    return {"cid": path[3],
            "pid": path[4],
            "file": path[5]};
}

function set_page_back_link(mode){
    const back_link = document.getElementById("page_back_btn");
    if(mode === "singlepage"){
        back_link.setAttribute("href", location.pathname.split('/').slice(0,-2).join('/'));
    }else{
        back_link.setAttribute("href", location.pathname.split('/').slice(0,-1).join('/'));
    }
}

window.addEventListener('DOMContentLoaded', function() {
    let actionurl = location.pathname;
    let answerform = document.getElementById("answerform");
    answerform.setAttribute("action", actionurl);

    const idinfo = get_ids();
    const url = contents_url + "?cid=" + idinfo["cid"] + "&pid=" + idinfo["pid"];
    fetch(url)
        .then((res) => {return res.json();})
        .then((result) => {
            console.log(result);
            set_page_back_link(result["control"]["type"]);
        })
        .catch((e) => {
            alert("Failed to get contents data.");
            console.log(e);
        });
});
