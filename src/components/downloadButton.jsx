function downloadFile(file, format) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("format", format);

    fetch("http://localhost:8000/convert-and-download", {
        method: "POST",
        body: formData,
    })
    .then(response => {
        if (!response.ok) throw new Error("변환 실패");
        return response.blob();
    })
    .then(blob => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `converted.${format}`;
        document.body.appendChild(a);
        a.click();
        a.remove();
    })
    .catch(err => alert("다운로드 오류: " + err.message));
}