package com.maxbl4.maxdialer.ui

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.maxbl4.maxdialer.data.PhoneData
import com.maxbl4.maxdialer.data.PhoneRepository
import com.maxbl4.maxdialer.data.SearchTarget
import com.maxbl4.maxdialer.search.SearchHit
import com.maxbl4.maxdialer.search.T9Search
import com.maxbl4.maxdialer.ui.recents.deleteBackward
import com.maxbl4.maxdialer.ui.recents.insert
import com.maxbl4.maxdialer.ui.recents.sanitized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class PhoneViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = PhoneRepository(app)

    /** Loads and writes run one at a time, so an older load never overwrites a newer one. */
    private val io = Dispatchers.IO.limitedParallelism(1)

    private val _data = MutableStateFlow(PhoneData())
    val data: StateFlow<PhoneData> = _data.asStateFlow()

    /** Number typed on the dialpad, with the cursor. */
    val dial: StateFlow<TextFieldValue> = typedNumber.asStateFlow()

    val query: StateFlow<String> = typedNumber.map { it.text }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, typedNumber.value.text)

    val results: StateFlow<List<SearchHit<SearchTarget>>> =
        combine(query, _data) { query, data -> T9Search.search(data.searchItems, query) }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val changes = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val watcher = repository.Watcher { changes.tryEmit(Unit) }

    init {
        changes.debounce(300).onEach { reload() }.launchIn(viewModelScope)
        reload()
    }

    /** Call on the main thread, also after permissions change. */
    fun reload() {
        watcher.update()
        viewModelScope.launch(io) { _data.value = repository.load() }
    }

    fun type(c: Char) = typedNumber.update { it.insert(c.toString()) }

    fun backspace() = typedNumber.update { it.deleteBackward() }

    /** Replaces the number; the cursor goes to the end. */
    fun setQuery(query: String) {
        typedNumber.value = TextFieldValue(query, TextRange(query.length))
    }

    /** Edits made in the number field itself: cursor moves, paste. */
    fun onDialChange(value: TextFieldValue) {
        typedNumber.value = value.sanitized()
    }

    fun deleteCalls(ids: Collection<Long>) {
        viewModelScope.launch(io) { repository.deleteCalls(ids) }
    }

    fun setFavorite(contactId: Long, starred: Boolean) {
        viewModelScope.launch(io) { repository.setStarred(contactId, starred) }
    }

    fun markMissedSeen() {
        if (_data.value.calls.none { it.isMissed && it.isNew }) return
        viewModelScope.launch(io) { repository.markMissedSeen() }
    }

    override fun onCleared() = watcher.stop()
}

/**
 * Kept for the life of the process rather than the screen, so the number survives the call screen
 * and leaving the app with Back.
 */
private val typedNumber = MutableStateFlow(TextFieldValue())

