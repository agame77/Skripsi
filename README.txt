1. Install software yang dibutuhkan yaitu eclipse dengan cara mengklik 2 kali installer yang sudah diberikan pada folder software
2. Import library yang dibutuhkan seperti ANT, commander, rt, rtx, jna, Apache dapat ditemukan didalam folder SandboxScenario
3. Add buildfiles pada eclipse dengan mengklik "Add buildfiles" lalu pilih build.xml dan buildUser.xml
4. Compile masing-masing node sensor dengan setting yang sesuai (nama, COM, Address) dengan eclipse melalui ANT pada eclipse lalu pilih skenario pada buildUser.xml
	setelah itu klik 2 kali pada .all di bagian build.xml untuk melakukan compile node sensor
	(note : masing-masing  COM pada skenario di buildUser.xml harus disamakan dengan node sensor yang akan di compile)
5. Karena yang dibuat jar adalah BaseStation pada COM3 pastikan COM3 dicompile sebagai BaseStation (jika tidak harus mengganti COM pada Tester dan dibuat ulang Tester.jar)
6. Klik 2 kali pada Start.bat untuk menjalankan program jika seluruh node sudah dicompile sesuai dengan skenarionya (skenario 1/2/3)
7. Pengguna dapat Check Online Node dengan input '1'
8. Pengguna dapat melakukan prediksi pada area rel kereta api dengan input '2'
9. Pengguna dapat memberhentikan prediksi, dengan masukan input '3'
10. Pengguna dapat memberhentikan program, dengan masukan input '4'