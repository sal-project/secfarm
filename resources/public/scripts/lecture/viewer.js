const contents_url = "/lecture/api/get-contents";
const progress_url = "/lecture/api/get-progress";
const mark_url = "/lecture/api/mark-completed";
let menu_visible = false;

function mark_completed(){
    if(document.getElementById("markbtn").getAttribute("disabled") === null){
        const mark = document.getElementById("mark");
        mark.setAttribute("class", "modal is-active");
    }
}

function mark_cancel(){
    const mark = document.getElementById("mark");
    mark.setAttribute("class", "modal");
}

function get_ids(){
    const path = location.pathname.split("/");
    return {"cid": path[3],
            "pid": path[4],
            "file": path[5]};
}

function isSmartPhone() {
    if (window.innerWidth < 880) {
        return true;
    } else {
        return false;
    }
}

function resize_iframe(){
    const fullheight =  document.documentElement.clientHeight;
    const navi = document.getElementById("navi");
    const screen = document.getElementById("screen");

    const naviheight = navi.clientHeight;
    screen.style.height = `${fullheight - naviheight}px`;
    
    //iframe.setAttribute("width",iframe.parentNode.clientWidth + "px");
}

function mark_completed_yes(){
    const mark = document.getElementById("mark");
    const ids = get_ids();
    const url = mark_url + "?cid=" + ids["cid"] + "&pid=" + ids["pid"] + "&content=" + ids["file"];
    fetch(url)
        .then((res) => {return res;})
        .then((result) => {
            console.log(result);
            mark.setAttribute("class", "modal");
            document.getElementById("markbtn").setAttribute("disabled", "true");
            
        })
        .catch((e) => {
            alert("Failed to update status.");
            console.log(e);
        });
}

function get_my_progress(result) {
    const ids = get_ids();
    const target = result.filter((itms) => itms["course_id"] === ids["cid"] && itms["package_id"] === ids["pid"] && itms["content_path"] === ids["file"]);
    if(target.length == 0){
        return 0;
    }else{
        return target[0]["progress"];
    }
}

function build_index_menu(infos){
    const ids = get_ids();
    const contentslist = document.getElementById("contentslist");

    infos.forEach((info) => {
        const link = document.createElement("a");
        const content = info["content"];
        link.setAttribute("href", `/lecture/c/${ids["cid"]}/${ids["pid"]}/${content}`);
        
        if(ids["file"] === content) {
            link.classList.add("has-background-primary-light");
        }
        
        if(info["nickname"] === undefined) {
            link.textContent = info["content"];
        }else{
            link.textContent = info["nickname"];
        }

        const li = document.createElement("li");
        li.appendChild(link);
        contentslist.appendChild(li);
    });
    if(!isSmartPhone()){
        document.getElementById("menuarea").classList.remove("is-hidden");
    }
    resize_iframe();
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
    const idinfo = get_ids();
    const url = progress_url + "?cid=" + idinfo["cid"];
    console.log(url);
    fetch(url)
        .then((res) => {return res.json();})
        .then((result) => {
            console.log(result);
            const progress = get_my_progress(result);
            if(progress === 100){
                document.getElementById("markbtn").setAttribute("disabled", "true");
            }
        })
        .catch((e) => {
            alert("Failed to get progress data.");
            console.log(e);
        });

    const url2 = contents_url + "?cid=" + idinfo["cid"] + "&pid=" + idinfo["pid"];
    fetch(url2)
        .then((res) => {return res.json();})
        .then((result) => {
            console.log(result);
            set_page_back_link(result["control"]["type"]);
            if(result["control"]["type"] === "simple") {
                menu_visible = true;
                build_index_menu(result["contents"]);
            }else{
                resize_iframe();
            }
        })
        .catch((e) => {
            alert("Failed to get contents data.");
            console.log(e);
        });
});

addEventListener("resize", (event) => {
    if(menu_visible){
        if(!isSmartPhone()){
            document.getElementById("menuarea").classList.remove("is-hidden");
        }else{
            document.getElementById("menuarea").classList.add("is-hidden");
        }
    }
    resize_iframe();
});
