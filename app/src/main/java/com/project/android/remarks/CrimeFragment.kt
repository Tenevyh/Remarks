package com.project.android.remarks

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.text.format.DateFormat
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = 0
private const val DIALOG_TIME = "DialogTime"
private const val REQUEST_TIME = 1
private const val DATE_FORMAT = "EEE, MMM, dd"
private const val REQUEST_CONTACT = 1
private const val REQUEST_PHOTO = 2

class CrimeFragment: Fragment(), DatePickerFragment.Callbacks, TimePickerFragment.Callbacks {

    private lateinit var treeObserver: ViewTreeObserver
    private var viewWidth = 0
    private var viewHeight = 0

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var timeButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var format1: SimpleDateFormat
    private lateinit var format2: SimpleDateFormat
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var callSuspect: Button
    private lateinit var photoButton: ImageButton
    private lateinit var photoView: ImageView
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri


    private val crimeDetailViewModel : CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        crimeDetailViewModel.loadCrime(crimeId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                             savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        timeButton = view.findViewById(R.id.crime_time)
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        callSuspect = view.findViewById(R.id.call_suspect) as Button
        photoButton = view.findViewById(R.id.crime_camera) as ImageButton
        photoView = view.findViewById(R.id.crime_photo) as ImageView

        treeObserver = photoView.viewTreeObserver
        treeObserver.addOnGlobalLayoutListener {
            viewWidth = photoView.width
            viewHeight = photoView.height
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer{
                crime ->
                crime?.let {
                    this.crime = crime
                    photoFile = crimeDetailViewModel.getPhotoFile(crime)
                    photoUri = FileProvider.getUriForFile(requireActivity(),
                        "com.project.android.remark.fileprovider",
                        photoFile)
                    updateUI()
                }
            })
    }

    override fun onStart(){
        super.onStart()

        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int,
                                           after: Int) {

            }

            override fun onTextChanged(sequence: CharSequence?, start: Int, before: Int,
                                       count: Int){
                crime.title = sequence.toString()
            }
            override fun afterTextChanged(sequence: Editable?){

            }
        }
        format1 = SimpleDateFormat("d.MM.yy hh:mm", Locale.ROOT)

        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply{
            setOnCheckedChangeListener{_, isCheked -> crime.isSolved = isCheked}
        }

        dateButton.setOnClickListener {
                DatePickerFragment.newInstance(crime.date).apply {
                    setTargetFragment(this@CrimeFragment, REQUEST_DATE)
                    show(this@CrimeFragment.parentFragmentManager, DIALOG_DATE)
                }
        }

        timeButton.setOnClickListener {
            TimePickerFragment.newInstance(crime.date).apply {
                setTargetFragment(this@CrimeFragment, REQUEST_TIME)
                show(this@CrimeFragment.parentFragmentManager, DIALOG_TIME)
            }
        }

        reportButton.setOnClickListener{
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plan"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crime_report_subject))
            }.also {
                intent ->
                val chooserActivity = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserActivity)
            }
        }

        suspectButton.apply {
         val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

            setOnClickListener{
                startActivityForResult(pickContactIntent, REQUEST_CONTACT)
            }
            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(pickContactIntent,
            PackageManager.MATCH_DEFAULT_ONLY)
           /*if (resolvedActivity == null){   // не работает должным образом, кнопка постоянно выключена, даже при наличии необходимых приложений
            isEnabled = false
              }
            */
        }

        photoButton.apply {
            val packageManager: PackageManager = requireActivity().packageManager

            val captureImage = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val resolvedActivity: ResolveInfo? = packageManager.resolveActivity(captureImage,
                PackageManager.MATCH_DEFAULT_ONLY)
           /* if(resolvedActivity == null) {  // не работает должным образом, кнопка постоянно выключена, даже при наличии необходимых приложений
                isEnabled = false
            }
            */

            setOnClickListener {
                captureImage.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

                val cameraActivities: List<ResolveInfo> =
                    packageManager.queryIntentActivities(captureImage,
                        PackageManager.MATCH_DEFAULT_ONLY)

                for (cameraActivity in cameraActivities) {
                    requireActivity().grantUriPermission(cameraActivity.activityInfo.packageName,
                        photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                }
                startActivityForResult(captureImage, REQUEST_PHOTO)
            }
        }

        callSuspect.setOnClickListener {
            val callContactIntent =
                Intent(Intent.ACTION_DIAL).apply {

                    val phone = crime.phone
                    data = Uri.parse("tel:$phone")

                }
            // это намерение вызовет номер телефона, указанный в Uri.parse("tel:$phone")
            startActivity(callContactIntent)
        }

        photoView.setOnClickListener {
            if (photoFile.length()<1) {
            } else{
            val fragment = FullScreen(photoFile)
            fragment.show(childFragmentManager, "PhotoFragmentDialog")
        }
        }
    }

    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    private fun updateUI(){
        titleField.setText(crime.title)
        format1 = SimpleDateFormat("dd.MM.y")
        dateButton.text = format1.format(crime.date)
        format2 = SimpleDateFormat("HH:mm")
        timeButton.text = format2.format(crime.date)
        solvedCheckBox.apply{
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        if(crime.suspect.isNotEmpty()){
            suspectButton.text = crime.suspect
        }
        updatePhotoView(viewWidth, viewHeight)
    }

    private fun updatePhotoView(width: Int, height: Int) {
        if (photoFile.exists()) {
            val bitmap = getScaledBitmap(photoFile.path, width, height)
            photoView.contentDescription = getString(R.string.crime_photo_image_description)
            photoView.setImageBitmap(bitmap)
        } else {
            photoView.setImageDrawable(null)
            photoView.contentDescription = getString(R.string.crime_photo_no_image_description)
        }
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().revokeUriPermission(photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when {
            resultCode != Activity.RESULT_OK -> return

            requestCode == REQUEST_CONTACT && data != null -> {
                val contactUri: Uri? = data.data

                var contactId: String? = null


                val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts._ID)
                val cursor = contactUri?.let { requireActivity().contentResolver.query(it, queryFields, null, null, null) }
                cursor?.use {
                    if(it.count == 0) return

                    it.moveToFirst()

                    val suspect = it.getString(0)
                    //получить идентификатор контакта
                    contactId = it.getString(1)

                    crime.suspect = suspect
                    suspectButton.text = suspect
                }

                // Это Uri, чтобы получить номер телефона
                val phoneURI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

                // phoneNumberQueryFields: список для возврата только столбца PhoneNumber
                val phoneNumberQueryFields = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)

                //phoneWhereClause: фильтр, объявляющий, какие строки возвращать, отформатированный как предложение SQL WHERE (за исключением самого WHERE)
                val phoneWhereClause = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"

                // Этот val заменяет вопросительный знак в phoneWhereClause val
                val phoneQueryParameters = arrayOf(contactId)

                val phoneCursor = requireActivity().contentResolver
                    .query(phoneURI, phoneNumberQueryFields, phoneWhereClause, phoneQueryParameters, null )

                //строка номера телефона
                var phoneNumber: String = ""

                val allNumbers: ArrayList<String> = arrayListOf<String>()
                allNumbers.clear()

                phoneCursor?.use {cursorPhone ->

                    cursorPhone.moveToFirst()
                    while (cursorPhone.isAfterLast == false)
                    {
                        phoneNumber = cursorPhone.getString(0)
                        allNumbers.add(phoneNumber)
                        cursorPhone.moveToNext()
                    }
                }


                val items = allNumbers.toTypedArray()

                var selectedNumber: String = ""


                val builder = AlertDialog.Builder(context)
                builder.setTitle("Choose a Number:")
                builder.setItems(items, DialogInterface.OnClickListener { dialog, which ->  selectedNumber = allNumbers[which].toString().replace("_","")
                    crime.phone = selectedNumber
                    callSuspect.text = crime.phone
                })

                val alert = builder.create()
                if(allNumbers.size > 1) {
                    alert.show()
                }
                else if (allNumbers.size == 1 && allNumbers[0].isNotEmpty()) {
                    selectedNumber = allNumbers[0].toString().replace("_","")
                    crime.phone = selectedNumber
                    callSuspect.text = crime.phone

                }

                else
                {
                    callSuspect.text = "no phone number found!"
                    crime.phone = ""
                }

                crimeDetailViewModel.saveCrime(crime)
            }

            requestCode == REQUEST_PHOTO -> {
                requireActivity().revokeUriPermission(photoUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                updatePhotoView(viewWidth, viewHeight)
            }

        }
    }

    private fun getCrimeReport(): String {
        val solvedString = if(crime.isSolved){
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        val suspect = if(crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect)
        }
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)
    }

    override fun onDateSelected(date: Date){
        crime.date = date
        updateUI()
    }

    override fun onTimeSelected(date: Date){
        crime.date = date
        updateUI()
    }

    companion object {

        fun newInstance(crimeID: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeID)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}