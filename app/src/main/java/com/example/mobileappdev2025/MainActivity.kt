package com.example.mobileappdev2025

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileInputStream
import java.util.Random
import java.util.Scanner

data class WordDefinition(val word: String, val definition: String);

class MainActivity : AppCompatActivity() {
    private val ADD_WORD_CODE = 1234; // 1-65535
    private lateinit var myAdapter : ArrayAdapter<String>; // connect from data to gui
    private var dataDefList = ArrayList<String>(); // data
    private var wordDefinition = mutableListOf<WordDefinition>();
    private var score : Int = 1;
    private var totalCorrect : Int = 2;
    private var totalWrong : Int = 3;
    private var streak: Int = 4;
    private var longestStreak: Int = 5;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadWordsFromDisk()

        pickNewWordAndLoadDataList();
        setupList();

        val defList = findViewById<ListView>(R.id.dynamic_def_list);
        defList.setOnItemClickListener { _, _, index, _ ->
            pickNewWordAndLoadDataList();
            myAdapter.notifyDataSetChanged();
        };
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_WORD_CODE && resultCode == RESULT_OK && data != null){
            val word = data.getStringExtra("word")?:""
            val def = data.getStringExtra("def")?:""

            Log.d("MAD", word)
            Log.d("MAD", def)

            if ( word == "" || def == "")
                return

            wordDefinition.add(WordDefinition(word, def))



            pickNewWordAndLoadDataList()
            myAdapter.notifyDataSetChanged()
        }
    }

    private fun loadWordsFromDisk()
    {
        // user data
        val file = File(applicationContext.filesDir, "user_data.csv")

        if (file.exists()) {
            val readResult = FileInputStream(file)
            val scanner = Scanner(readResult)

            while(scanner.hasNextLine()){
                val line = scanner.nextLine()
                val wd = line.split("|")
                wordDefinition.add(WordDefinition(wd[0], wd[1]))
            }
        } else { // default data

            file.createNewFile()

            val reader = Scanner(resources.openRawResource(R.raw.default_words))
            while(reader.hasNextLine()){
                val line = reader.nextLine()
                val wd = line.split("|")
                wordDefinition.add(WordDefinition(wd[0], wd[1]))
                file.appendText("${wd[0]}|${wd[1]}\n")
            }
        }
    }

    private fun pickNewWordAndLoadDataList()
    {
        if (wordDefinition.isEmpty()) {
            Log.e("ERROR", "def list empty")
            return
        }

        wordDefinition.shuffle()

        dataDefList.clear()


        val correctWord = wordDefinition[0].word
        val correctDefinition = wordDefinition[0].definition

        dataDefList.add(correctDefinition)


        val incorrectDefinitions = wordDefinition
            .filter { it.definition != correctDefinition }
            .map { it.definition }
            .shuffled()

        // Add only three incorrect answers (or as many as possible)
        dataDefList.addAll(incorrectDefinitions.take(3))


        while (dataDefList.size < 4) {
            dataDefList.add("definition")
        }

        dataDefList.shuffle()


        findViewById<TextView>(R.id.word).text = correctWord


        if (::myAdapter.isInitialized) {
            myAdapter.notifyDataSetChanged()
        } else {
            Log.e("ERROR", "adapapter not working")
        }
    }

    private fun setupList()
    {
        myAdapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dataDefList);

        // connect to list
        val defList = findViewById<ListView>(R.id.dynamic_def_list);
        defList.adapter = myAdapter;
    }

    fun openStats(view : View)
    {
        var myIntent = Intent(this, StatsActivity::class.java);
        myIntent.putExtra("score", score.toString());
        myIntent.putExtra("totalCorrect", totalCorrect.toString());
        myIntent.putExtra("totalWrong", totalWrong.toString());
        startActivity(myIntent)
    }

    fun openAddWord(view : View)
    {
        var myIntent = Intent(this, AddWordActivity::class.java);
        startActivityForResult(myIntent, ADD_WORD_CODE)
    }
}