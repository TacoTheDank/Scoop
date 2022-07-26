@file:JvmName("CrashDatabase")

package taco.scoop.core.db

import android.content.Context
import com.afollestad.inquiry.Inquiry
import taco.scoop.core.data.crash.Crash

private fun getDatabase(instanceName: String): Inquiry {
    return Inquiry.get(instanceName)
}

fun createDatabaseInstance(context: Context, instanceName: String?) {
    Inquiry.newInstance(context, "crashes")
        .instanceName(instanceName)
        .build()
}

fun destroyDatabaseInstance(instanceName: String?) {
    Inquiry.destroy(instanceName)
}

fun dropTable() {
    getDatabase("main")
        .dropTable(Crash::class.java)
}

fun deleteWhereIn(vararg selectionArgs: Any?) {
    getDatabase("main")
        .delete(Crash::class.java)
        .whereIn("_id", selectionArgs)
        .run()
}

fun deleteValues(values: List<Crash>) {
    getDatabase("main")
        .delete(Crash::class.java)
        .values(values)
        .run()
}

fun insertValues(values: List<Crash?>) {
    getDatabase("receiver")
        .insert(Crash::class.java)
        .values(values)
        .run()
}

fun selectAll(): Array<out Crash>? {
    return getDatabase("main")
        .select(Crash::class.java)
        .all()
}
