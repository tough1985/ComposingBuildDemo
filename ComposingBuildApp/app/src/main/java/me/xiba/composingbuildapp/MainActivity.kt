package me.xiba.composingbuildapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import me.xiba.composinglibrary.Constant

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 引入ComposingBuildLibrary的Constant.TAG
        findViewById<TextView>(R.id.tv_content).text = Constant.TAG
    }
}