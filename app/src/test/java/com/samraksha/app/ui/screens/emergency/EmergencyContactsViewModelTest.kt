package com.samraksha.app.ui.screens.emergency

import com.samraksha.app.domain.model.EmergencyContact
import com.samraksha.app.domain.repository.EmergencyContactRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmergencyContactsViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeRepository: FakeEmergencyContactRepo
    private lateinit var viewModel: EmergencyContactsViewModel

    private val sampleContact = EmergencyContact(
        id = "c1",
        name = "Deepak Sharma",
        phoneNumber = "+919876543210",
        relationship = "Brother"
    )

    @Before
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeEmergencyContactRepo()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): EmergencyContactsViewModel {
        return EmergencyContactsViewModel(fakeRepository)
    }

    @Test
    fun initialState_loadsContactsFromRepository() = runTest(testDispatcher) {
        fakeRepository.setContacts(listOf(sampleContact))
        viewModel = createViewModel()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(1, state.contacts.size)
        assertEquals("Deepak Sharma", state.contacts.first().name)
        assertFalse(state.isAddingContact)
        assertNull(state.error)
    }

    @Test
    fun saveContact_withEmptyName_setsValidationError() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.showAddContactDialog()
        viewModel.updateNewName("   ")
        viewModel.updateNewPhone("+919876543210")
        viewModel.updateNewRelationship("Father")
        viewModel.saveContact()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isAddingContact)
        assertEquals("Contact name cannot be empty", state.error)
        assertEquals(0, fakeRepository.contactsList.size)
    }

    @Test
    fun saveContact_withShortName_setsValidationError() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.showAddContactDialog()
        viewModel.updateNewName("A")
        viewModel.updateNewPhone("+919876543210")
        viewModel.updateNewRelationship("Father")
        viewModel.saveContact()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isAddingContact)
        assertEquals("Please enter a valid contact name (at least 2 characters)", state.error)
    }

    @Test
    fun saveContact_withInvalidPhoneNumber_setsValidationError() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.showAddContactDialog()
        viewModel.updateNewName("Anita Verma")
        viewModel.updateNewPhone("12345") // Only 5 digits
        viewModel.updateNewRelationship("Mother")
        viewModel.saveContact()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isAddingContact)
        assertEquals("Please enter a valid phone number with at least 10 digits", state.error)
        assertEquals(0, fakeRepository.contactsList.size)
    }

    @Test
    fun saveContact_withEmptyRelationship_setsValidationError() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.showAddContactDialog()
        viewModel.updateNewName("Anita Verma")
        viewModel.updateNewPhone("+919876543210")
        viewModel.updateNewRelationship("   ")
        viewModel.saveContact()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isAddingContact)
        assertEquals("Relationship is required (e.g., Parent, Friend, Doctor)", state.error)
    }

    @Test
    fun saveContact_withValidInputs_persistsToRepositoryAndClosesDialog() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.showAddContactDialog()
        viewModel.updateNewName("Ramesh Kumar")
        viewModel.updateNewPhone("+919876543210")
        viewModel.updateNewRelationship("Father")
        viewModel.saveContact()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isAddingContact)
        assertNull(state.error)
        assertEquals(1, fakeRepository.contactsList.size)
        assertEquals("Ramesh Kumar", fakeRepository.contactsList.first().name)
        assertEquals("+919876543210", fakeRepository.contactsList.first().phoneNumber)
        assertEquals("Father", fakeRepository.contactsList.first().relationship)
    }

    @Test
    fun editContact_updatesExistingContactInRepository() = runTest(testDispatcher) {
        fakeRepository.setContacts(listOf(sampleContact))
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.showEditContactDialog(sampleContact)
        viewModel.updateNewName("Deepak Sharma (Work)")
        viewModel.updateNewPhone("+919988776655")
        viewModel.saveContact()
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertFalse(state.isAddingContact)
        assertEquals(1, fakeRepository.contactsList.size)
        assertEquals("Deepak Sharma (Work)", fakeRepository.contactsList.first().name)
        assertEquals("+919988776655", fakeRepository.contactsList.first().phoneNumber)
    }

    @Test
    fun deleteContact_removesContactFromRepository() = runTest(testDispatcher) {
        fakeRepository.setContacts(listOf(sampleContact))
        viewModel = createViewModel()
        testScheduler.runCurrent()

        assertEquals(1, viewModel.uiState.value.contacts.size)

        viewModel.deleteContact(sampleContact)
        testScheduler.runCurrent()

        assertEquals(0, fakeRepository.contactsList.size)
    }

    @Test
    fun setContactFromPicker_prepopulatesDialogFields() = runTest(testDispatcher) {
        viewModel = createViewModel()
        testScheduler.runCurrent()

        viewModel.setContactFromPicker("Kavita Rao", "+919123456789")
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.isAddingContact)
        assertEquals("Kavita Rao", state.newName)
        assertEquals("+919123456789", state.newPhone)
        assertNull(state.error)
    }
}

// ── Test Fakes ────────────────────────────────────────────────────────────────

private class FakeEmergencyContactRepo : EmergencyContactRepository {
    val contactsList = mutableListOf<EmergencyContact>()
    private val contactsFlow = MutableStateFlow<List<EmergencyContact>>(emptyList())

    fun setContacts(list: List<EmergencyContact>) {
        contactsList.clear()
        contactsList.addAll(list)
        contactsFlow.value = contactsList.toList()
    }

    override fun getEmergencyContacts(): Flow<List<EmergencyContact>> = contactsFlow.asStateFlow()

    override suspend fun addContact(contact: EmergencyContact) {
        contactsList.add(contact)
        contactsFlow.value = contactsList.toList()
    }

    override suspend fun updateContact(contact: EmergencyContact) {
        val index = contactsList.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            contactsList[index] = contact
            contactsFlow.value = contactsList.toList()
        }
    }

    override suspend fun deleteContact(contact: EmergencyContact) {
        contactsList.removeAll { it.id == contact.id }
        contactsFlow.value = contactsList.toList()
    }

    override suspend fun syncContacts(): Result<List<EmergencyContact>> = Result.success(contactsList.toList())
}
