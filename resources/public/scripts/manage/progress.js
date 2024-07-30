const userlist_url = "/manage/api/get-userlist";
const progress_url = "/manage/api/get-user-progress";
const test_reset_url = "/manage/api/reset-test-record";
const progress_reset_url = "/manage/api/reset-content-progress";

var update_info = [];

function create_userlist() {
    fetch(userlist_url)
        .then((res) => {return res.json();})
        .then((result) => {
            let select = document.getElementById("user_selection");
            let options = result.map((user) => {
                let option = document.createElement("option");
                option.text = user.username;
                option.value = user.uid;
                return option;
            });
            options.forEach((opt) => {select.appendChild(opt);});
        })
        .catch((e) => {
            alert("Failed to fetch user information");
            console.log(e);
        });
}

function clear_tbody(target_tbody) {
    while (target_tbody.firstChild) {
        target_tbody.removeChild(target_tbody.firstChild);
    }
}

function apply_table(info){
    const selected_uid = document.getElementById("user_selection").value;
    const uid = parseInt(selected_uid, 10);
    
    let prog_table = document.getElementById("progress_table");
    let prog_thead = document.getElementById("progress_table_head");
    let prog_tbody = document.getElementById("progress_table_body");

    clear_tbody(prog_tbody);

    info.forEach((col) => {
        let tr = prog_tbody.insertRow();
        let tr_cid = document.createElement("th");
        let tr_pid = document.createElement("th");
        let tr_path = document.createElement("th");
        
        tr_cid.innerHTML = col["cid"];
        tr_pid.innerHTML = col["pid"];
        tr_path.innerHTML = col["content"];
        tr.appendChild(tr_cid);
        tr.appendChild(tr_pid);
        tr.appendChild(tr_path);

        let type_cell = tr.insertCell();
        type_cell.innerHTML = col["type"];
        
        let prog_cell = tr.insertCell();
        if(col["type"] === "doc") {
            prog_cell.innerHTML = col["progress"];
        }else{
            prog_cell.innerHTML = "-";
        }

        let score_cell = tr.insertCell();
        if(col["type"] === "test"){
            if(col["scoremax"] !== null){
                const score = Math.round(col["score"]/col["scoremax"] * 100);
                score_cell.innerHTML = `${col["score"]}/${col["scoremax"]} (${score}%)`;
            }else{
                score_cell.innerHTML = "N";
            }
        }else{
            score_cell.innerHTML = "-";
        }
        
        let input_reset = document.createElement("input");
        input_reset.type = "checkbox";
        input_reset.checked = col['show'];
        input_reset.addEventListener('change', (event) => {
            if (event.currentTarget.checked) {
                update_info.push({"uid": uid,
                                  "cid": col["cid"],
                                  "pid": col["pid"],
                                  "content": col["content"],
                                  "type": col["type"]});
            } else {
                update_info = update_info.filter((i) => {
                    return !(i["cid"] === col["cid"] && i["pid"] === col["pid"] && i["content"] === col["content"]);
                });
            }
        });
        let reset_cell = tr.insertCell();
        reset_cell.appendChild(input_reset);
    });
}

function render_progress_table(uid) {
    fetch(progress_url + "?uid=" + uid)
        .then((res) => {return res.json();})
        .then((result) => {
            console.log(result);
            apply_table(result);
        })
        .catch((e) => {
            alert("Failed to fetch progress data");
            console.log(e);
        });
}

function select_user() {
    const selected_uid = document.getElementById("user_selection").value;
    const uid = parseInt(selected_uid, 10);
    if(!isNaN(uid)){
        console.log(`selected = ${uid}`);
        update_info = [];
        if(uid > 0) {
            document.getElementById("progress_viewer_area").hidden = false;
            render_progress_table(uid);
        }else {
            document.getElementById("progress_viewer_area").hidden = true;
        }
    }else{
        alert("Something seems to be wrong with the selected item");
    }
}

function apply_reset(){
    if(update_info.length > 0){
        const mark = document.getElementById("mark");
        mark.setAttribute("class", "modal is-active");
    }else{
        alert("Target is not selected.");
    }
}

function reset_cancel(){
    const mark = document.getElementById("mark");
    mark.setAttribute("class", "modal");
}

function reset_yes(){
    update_info.forEach((info) => {
        const props = `?uid=${info["uid"]}&cid=${info["cid"]}&pid=${info["pid"]}&content=${info["content"]}`;
        const target = info["type"] === "doc" ? progress_reset_url : test_reset_url;
        fetch(target + props)
            .then((res) => {return res.json();})
            .then((result) => {
                console.log(result);
                update_info = [];
                reset_cancel();
                location.reload();
            })
            .catch((e) => {
                alert("Failed to reset operation");
                console.log(e);
            });
    });
}


window.addEventListener('DOMContentLoaded', function() {
    create_userlist();
});

