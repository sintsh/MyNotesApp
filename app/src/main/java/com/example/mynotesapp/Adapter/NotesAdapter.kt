package com.example.mynotesapp.Adapter

import android.content.Context
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.mynotesapp.Models.Note
import com.example.mynotesapp.R
import kotlin.random.Random

class NotesAdapter(
    private val context: Context ,
    val listener: NotesClickListener

): RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {


    private val notesList = ArrayList<Note>()
    private val fullList = ArrayList<Note>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NoteViewHolder {
        return NoteViewHolder(
            LayoutInflater.from(context).inflate(R.layout.list_item,parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: NoteViewHolder,
        position: Int
    ) {
        val currentNote = notesList[position]

        holder.tv_title.text = currentNote.title
        holder.tv_note.text = currentNote.note
        holder.tv_date.text = currentNote.date

        holder.tv_title.isSelected = true
        holder.tv_date.isSelected = true

        holder.note_layout.setCardBackgroundColor(holder.itemView.resources.getColor(randomColor(),null))

        holder.note_layout.setOnClickListener {
            listener.onItemClicked(notesList[holder.adapterPosition])
        }

        holder.note_layout.setOnLongClickListener {
            listener.onLongItemClicked(
                notesList[holder.adapterPosition],
                holder.note_layout
            )
            true
        }

    }


    fun  randomColor():Int{
        val list = ArrayList<Int>()
        list.add(R.color.NoteColor1)
        list.add(R.color.NoteColor2)
        list.add(R.color.NoteColor3)
        list.add(R.color.NoteColor4)
        list.add(R.color.NoteColor5)
        list.add(R.color.NoteColor6)

        val seed = System.currentTimeMillis().toInt()
        val randomIndex = Random(seed).nextInt(list.size)
        return list[randomIndex]
    }
    fun updateList(newList: List<Note>){
        fullList.clear()
        fullList.addAll(newList)

        notesList.clear()
        notesList.addAll(fullList)

        notifyDataSetChanged()
    }

    fun filterList(search: String){
        notesList.clear()
        for (item in fullList){
            if(item.title?.lowercase()?.contains(search.lowercase()) ==true ||
                item.note?.lowercase()?.contains(search.lowercase())==true){
                notesList.add(item)
            }
        }

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
       return  notesList.size
    }

    inner class NoteViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){

        val note_layout = itemView.findViewById<CardView>(R.id.card_layout)

        val tv_title = itemView.findViewById<TextView>(R.id.tv_title)



        val tv_note = itemView.findViewById<TextView>(R.id.tv_note)

        val tv_date = itemView.findViewById<TextView>(R.id.tv_date)

    }
    interface  NotesClickListener{

        fun onItemClicked(note:Note)
        fun onLongItemClicked(note:Note,cardView: CardView)

    }
}