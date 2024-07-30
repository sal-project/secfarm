const progress_url = "/my/history-data";

var update_info = [];

function clear_tbody(target_tbody) {
    while (target_tbody.firstChild) {
        target_tbody.removeChild(target_tbody.firstChild);
    }
}

function apply_table(info){
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
        
    });
}

function render_progress_table() {
    fetch(progress_url)
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

window.addEventListener('DOMContentLoaded', function() {
    render_progress_table();
});

