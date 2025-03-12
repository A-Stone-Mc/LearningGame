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

data class WordDefinition(val word: String, val definition: String, var streak: Int = 0);

class MainActivity : AppCompatActivity() {
    private val ADD_WORD_CODE = 1234; // 1-65535
    private lateinit var myAdapter : ArrayAdapter<String>; // connect from data to gui
    private var dataDefList = ArrayList<String>(); // data
    private var wordDefinition = mutableListOf<WordDefinition>();
    private var score : Int = 0;
    private var totalCorrect : Int = 0;
    private var totalWrong : Int = 0;
    private var streak: Int = 0;
    private var longestStreak: Int = 0;

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
            val selectedDef = dataDefList[index]
            val correctWordDef = wordDefinition.find { it.word == findViewById<TextView>(R.id.word).text.toString() }

            if (correctWordDef != null) {
                if (selectedDef == correctWordDef.definition) {
                    totalCorrect++
                    streak++
                    score += streak
                    correctWordDef.streak++

                    if (streak > longestStreak) {
                        longestStreak = streak
                    }
                } else {
                    correctWordDef.streak = 0
                    totalWrong++
                    streak = 0
                }

                saveWordsOnDisk()
                pickNewWordAndLoadDataList()
                myAdapter.notifyDataSetChanged()


                findViewById<TextView>(R.id.main_score_text).text = "Score: $score"
            }
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
            val file = File(applicationContext.filesDir, "user_data.csv")
            file.appendText("$word|$def\n")



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
                val streak = if (wd.size > 2) wd[2].toInt() else 0
                wordDefinition.add(WordDefinition(wd[0], wd[1], streak))
            }
        } else { // default data

            file.createNewFile()

            val reader = Scanner(resources.openRawResource(R.raw.original_words))
            while(reader.hasNextLine()){
                val line = reader.nextLine()
                val wd = line.split("|")
                wordDefinition.add(WordDefinition(wd[0], wd[1], 0))
                file.appendText("${wd[0]}|${wd[1]}|0\n")
            }
        }
    }

    private fun saveWordsOnDisk(){
        val file = File(applicationContext.filesDir, "user_data.csv")
        file.writeText("") // clear file

        for (wordDef in wordDefinition) {
            file.appendText("${wordDef.word}|${wordDef.definition}|${wordDef.streak}\n")
        }
    }

    private fun pickNewWordAndLoadDataList()
    {
        if (wordDefinition.isEmpty()) {
            Log.e("ERROR", "def list empty")
            return
        }

        val filteredWords = wordDefinition.filter { it.streak < 2 }
        val freqList = if (filteredWords.isNotEmpty()) {
            filteredWords.toMutableList()
        } else {
            wordDefinition.toMutableList()
        }.shuffled().toMutableList()

        freqList.shuffle()

        dataDefList.clear()


        val correctWord = freqList[0].word
        val correctDefinition = freqList[0].definition

        dataDefList.add(correctDefinition)


        val incorrectDefinitions = wordDefinition
            .filter { it.definition != correctDefinition }
            .map { it.definition }
            .shuffled()

        //add 3 incorrect
        dataDefList.addAll(incorrectDefinitions.take(3))


        while (dataDefList.size < 4) {
            dataDefList.add("definition")
        }

        dataDefList.shuffle()


        findViewById<TextView>(R.id.word).text = correctWord

        findViewById<TextView>(R.id.main_score_text).text = "Score: $score"


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
        myIntent.putExtra("streak", streak.toString())
        myIntent.putExtra("longestStreak", longestStreak.toString())
        startActivity(myIntent)
    }

    fun openAddWord(view : View)
    {
        var myIntent = Intent(this, AddWordActivity::class.java);
        startActivityForResult(myIntent, ADD_WORD_CODE)
    }
}