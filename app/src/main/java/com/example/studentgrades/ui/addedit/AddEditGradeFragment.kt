package com.example.studentgrades.ui.addedit

import android.app.DatePickerDialog
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.studentgrades.R
import com.example.studentgrades.data.AppDatabase
import com.example.studentgrades.data.model.Discipline
import com.example.studentgrades.data.model.Grade
import com.example.studentgrades.databinding.FragmentAddEditGradeBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Фрагмент экрана добавления новой или редактирования существующей оценки
class AddEditGradeFragment : Fragment() {

    // Ссылка на ViewBinding (живёт только между onCreateView и onDestroyView)
    private var _binding: FragmentAddEditGradeBinding? = null
    // Безопасный геттер для доступа к элементам интерфейса без проверок на null
    private val binding get() = _binding!!

    // ID дисциплины, если экран открыт из карточки конкретного предмета (-1L = не выбрана)
    private var preselectedDisciplineId: Long = -1L
    // ID оценки, если экран открыт в режиме редактирования (-1L = создание новой оценки)
    private var editingGradeId: Long = -1L
    // Кэш списка дисциплин, загруженных из базы данных для выпадающего списка
    private var disciplinesList: List<Discipline> = emptyList()
    // Текущий выбранный тип учебной работы (по умолчанию — контрольная)
    private var selectedWorkType: String = "Контрольная работа"
    // Выбранная дата оценки в миллисекундах (по умолчанию — текущее время)
    private var selectedDateMillis: Long = System.currentTimeMillis()
    // Форматтер для отображения даты в формате «ДД.ММ.ГГГГ»
    private val formatter = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Считываем переданные при навигации аргументы (ID дисциплины и ID оценки)
        preselectedDisciplineId = arguments?.getLong("disciplineId", -1L) ?: -1L
        editingGradeId = arguments?.getLong("gradeId", -1L) ?: -1L
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // «Надуваем» (создаём) разметку экрана из XML через ViewBinding
        _binding = FragmentAddEditGradeBinding.inflate(inflater, container, false)
        // Возвращаем корневой View фрагмента
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Назначаем возврат на предыдущий экран по нажатию на кнопки «Назад» и «Отмена»
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.headerBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnCancel.setOnClickListener { findNavController().popBackStack() }

        // Настраиваем поле ввода оценки и кнопки быстрого выбора (2, 3, 4, 5)
        setupGradeInputAndQuickButtons()
        // Настраиваем кнопки выбора типа учебной работы
        setupWorkTypeButtons()
        // Настраиваем выбор даты через системный календарь
        setupDatePicker()
        // Загружаем список дисциплин и (при редактировании) данные самой оценки из БД
        loadInitialData()

        // Запускаем валидацию и сохранение по нажатию на кнопку «Сохранить»
        binding.btnSaveGrade.setOnClickListener { validateAndSave() }
    }

    // Настройка поля ввода оценки и кнопок быстрого выбора 2–5
    private fun setupGradeInputAndQuickButtons() {
        // При любом изменении текста в поле оценки скрываем сообщение об ошибке и возвращаем обычный фон
        binding.etGradeValue.doAfterTextChanged {
            binding.tvGradeError.visibility = View.GONE
            binding.etGradeValue.setBackgroundResource(R.drawable.bg_input_rounded)
        }

        // Связываем каждую кнопку быстрого выбора со строковым значением оценки
        val quickMap = mapOf(
            binding.btnGrade2 to "2",
            binding.btnGrade3 to "3",
            binding.btnGrade4 to "4",
            binding.btnGrade5 to "5"
        )
        // Вешаем обработчик нажатия на каждую кнопку из словаря
        quickMap.forEach { (btn, value) ->
            btn.setOnClickListener {
                // Подставляем цифру в поле ввода
                binding.etGradeValue.setText(value)
                // Перемещаем курсор в конец текста
                binding.etGradeValue.setSelection(value.length)
                // Скрываем текст ошибки и сбрасываем красную рамку
                binding.tvGradeError.visibility = View.GONE
                binding.etGradeValue.setBackgroundResource(R.drawable.bg_input_rounded)
            }
        }
    }

    // Настройка обработчиков нажатия для кнопок типа работы
    private fun setupWorkTypeButtons() {
        val buttons = getWorkTypeButtons()
        buttons.forEach { (button, typeName) ->
            button.setOnClickListener {
                // Запоминаем выбранный тип работы
                selectedWorkType = typeName
                // Обновляем визуальную подсветку кнопок
                updateWorkTypeSelection(buttons)
            }
        }
        // Выделяем кнопку, выбранную по умолчанию
        updateWorkTypeSelection(buttons)
    }

    // Возвращает список пар: «Кнопка в интерфейсе -> Название типа работы»
    private fun getWorkTypeButtons(): List<Pair<MaterialButton, String>> = listOf(
        binding.btnTypeControl to "Контрольная работа",
        binding.btnTypePractice to "Практическая работа",
        binding.btnTypeLab to "Лабораторная работа",
        binding.btnTypeAnswer to "Ответ на занятии"
    )

    // Обновляет цвета фона, текста и рамки кнопок типа работы в зависимости от выбора
    private fun updateWorkTypeSelection(buttons: List<Pair<MaterialButton, String>>) {
        val ctx = requireContext()
        // Получаем цвета для активного и неактивного состояний из ресурсов
        val selectedBg = ContextCompat.getColor(ctx, R.color.primary_blue_light)
        val selectedText = ContextCompat.getColor(ctx, R.color.primary_blue)
        val normalBg = ContextCompat.getColor(ctx, R.color.bg_card)
        val normalText = ContextCompat.getColor(ctx, R.color.text_secondary)

        buttons.forEach { (button, typeName) ->
            // Проверяем совпадение (включая сокращённые названия из старых записей БД)
            val isSelected = typeName.equals(selectedWorkType, ignoreCase = true) ||
                    (typeName.startsWith("Контрольная") && selectedWorkType.startsWith("Контрольная")) ||
                    (typeName.startsWith("Практическая") && selectedWorkType.startsWith("Практическая")) ||
                    (typeName.startsWith("Лабораторная") && selectedWorkType.startsWith("Лабораторная")) ||
                    (typeName.startsWith("Ответ") && selectedWorkType.startsWith("Ответ"))

            // Меняем заливку, цвет текста и цвет обводки кнопки
            button.backgroundTintList = ColorStateList.valueOf(if (isSelected) selectedBg else normalBg)
            button.setTextColor(if (isSelected) selectedText else normalText)
            button.strokeColor = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, if (isSelected) R.color.primary_blue else R.color.stroke_subtle)
            )
        }
    }

    // Настройка поля даты и открытие диалога выбора даты (DatePickerDialog)
    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        selectedDateMillis = calendar.timeInMillis
        // Отображаем сегодняшнюю дату в поле при открытии экрана
        binding.etDate.setText(formatter.format(Date(selectedDateMillis)))

        binding.etDate.setOnClickListener {
            // Устанавливаем в календаре текущую выбранную дату
            calendar.timeInMillis = selectedDateMillis
            // Открываем системное окно выбора даты
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    // Обновляем объект Calendar выбранными годом, месяцем и днём
                    calendar.set(year, month, dayOfMonth)
                    selectedDateMillis = calendar.timeInMillis
                    // Выводим отформатированную дату в текстовое поле
                    binding.etDate.setText(formatter.format(Date(selectedDateMillis)))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    // Асинхронная загрузка списка дисциплин и редактируемой оценки из Room
    private fun loadInitialData() {
        val db = AppDatabase.getInstance(requireContext())
        // Запускаем корутину, привязанную к жизненному циклу View фрагмента
        viewLifecycleOwner.lifecycleScope.launch {
            // В фоновом потоке (Dispatchers.IO) запрашиваем все дисциплины из БД
            disciplinesList = withContext(Dispatchers.IO) {
                db.disciplineDao().getAll()
            }

            // Если дисциплин нет, показывать форму бессмысленно — закрываем экран
            if (disciplinesList.isEmpty()) {
                Toast.makeText(requireContext(), "Сначала добавьте дисциплину", Toast.LENGTH_LONG).show()
                findNavController().popBackStack()
                return@launch
            }

            // Создаём стандартный адаптер для выпадающего списка (Spinner) с названиями предметов
            val spinnerAdapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                disciplinesList.map { it.name }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            binding.spinnerDiscipline.adapter = spinnerAdapter

            // Если передан editingGradeId != -1L — работаем в режиме редактирования
            if (editingGradeId != -1L) {
                binding.tvFormTitle.text = "Редактировать оценку"
                // Загружаем редактируемую оценку из БД в фоновом потоке
                val grade = withContext(Dispatchers.IO) {
                    db.gradeDao().getById(editingGradeId)
                }
                if (grade != null) {
                    // Заполняем все поля формы сохранёнными значениями
                    binding.etGradeValue.setText(grade.value.toString())
                    selectedDateMillis = grade.date
                    binding.etDate.setText(formatter.format(Date(selectedDateMillis)))
                    binding.etComment.setText(grade.comment.orEmpty())
                    selectedWorkType = grade.workType
                    updateWorkTypeSelection(getWorkTypeButtons())

                    // Выбираем в выпадающем списке дисциплину этой оценки
                    val discIndex = disciplinesList.indexOfFirst { it.id == grade.disciplineId }
                    if (discIndex >= 0) binding.spinnerDiscipline.setSelection(discIndex)
                }
            } else if (preselectedDisciplineId != -1L) {
                // Если создаём новую оценку из конкретного предмета — сразу выбираем его в Spinner
                val discIndex = disciplinesList.indexOfFirst { it.id == preselectedDisciplineId }
                if (discIndex >= 0) binding.spinnerDiscipline.setSelection(discIndex)
            }
        }
    }

    // Проверка корректности введённых данных и сохранение оценки в базу данных
    private fun validateAndSave() {
        // Сбрасываем визуальное состояние ошибки перед новой проверкой
        binding.tvGradeError.visibility = View.GONE
        binding.etGradeValue.setBackgroundResource(R.drawable.bg_input_rounded)

        // Считываем текст из поля оценки и пробуем преобразовать в целое число
        val valueText = binding.etGradeValue.text?.toString()?.trim().orEmpty()
        val gradeValue = valueText.toIntOrNull()

        // Проверяем, что введено число строго в диапазоне от 2 до 5
        if (gradeValue == null || gradeValue !in 2..5) {
            // Показываем ошибку и подсвечиваем поле красной рамкой
            binding.tvGradeError.visibility = View.VISIBLE
            binding.etGradeValue.setBackgroundResource(R.drawable.bg_input_error)
            return
        }

        // Получаем выбранную в Spinner дисциплину и очищенный текст комментария (или null, если пусто)
        val selectedDiscipline = disciplinesList[binding.spinnerDiscipline.selectedItemPosition]
        val commentText = binding.etComment.text?.toString()?.trim()?.ifEmpty { null }

        // Формируем объект Grade: с существующим id при обновлении или с автогенерацией id при создании
        val gradeToSave = if (editingGradeId != -1L) {
            Grade(
                id = editingGradeId,
                disciplineId = selectedDiscipline.id,
                value = gradeValue,
                date = selectedDateMillis,
                workType = selectedWorkType,
                comment = commentText
            )
        } else {
            Grade(
                disciplineId = selectedDiscipline.id,
                value = gradeValue,
                date = selectedDateMillis,
                workType = selectedWorkType,
                comment = commentText
            )
        }

        // Выполняем запись в Room в фоновом потоке IO
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val gradeDao = AppDatabase.getInstance(requireContext()).gradeDao()
            if (editingGradeId != -1L) {
                // Обновляем существующую запись
                gradeDao.update(gradeToSave)
            } else {
                // Вставляем новую запись
                gradeDao.insert(gradeToSave)
            }

            // Переключаемся обратно в главный UI-поток для показа уведомления и закрытия экрана
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Оценка сохранена", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем ссылку на binding при уничтожении View во избежание утечек памяти
        _binding = null
    }
}