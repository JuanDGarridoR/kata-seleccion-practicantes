// Consulta de Power Query (M) para Power BI Desktop.
// Inicio > Transformar datos > Nueva consulta > Consulta en blanco > Editor avanzado > pegar.
// La primera vez Power BI pide credenciales: elegir "Básica" -> analista / Analista2026*
let
    Origen = Json.Document(Web.Contents("http://localhost:8080/api/reportes/dataset")),
    Tabla = Table.FromList(Origen, Splitter.SplitByNothing(), {"Registro"}, null, ExtraValues.Error),
    Expandida = Table.ExpandRecordColumn(
        Tabla,
        "Registro",
        {"id", "carrera", "semestre", "estado", "fechaRegistro", "fechaDecision", "diasHastaDecision"}
    ),
    Tipos = Table.TransformColumnTypes(
        Expandida,
        {
            {"id", Int64.Type},
            {"carrera", type text},
            {"semestre", Int64.Type},
            {"estado", type text},
            {"fechaRegistro", type datetime},
            {"fechaDecision", type datetime},
            {"diasHastaDecision", type number}
        },
        "en-US"
    ),
    ConEstadoLegible = Table.AddColumn(
        Tipos,
        "Estado",
        each if [estado] = "VIABLE" then "Viable" else if [estado] = "NO_VIABLE" then "No viable" else "Pendiente",
        type text
    ),
    ConOrdenEmbudo = Table.AddColumn(
        ConEstadoLegible,
        "OrdenEstado",
        each if [estado] = "PENDIENTE" then 1 else if [estado] = "VIABLE" then 2 else 3,
        Int64.Type
    ),
    ConFecha = Table.AddColumn(ConOrdenEmbudo, "FechaRegistroDia", each DateTime.Date([fechaRegistro]), type date)
in
    ConFecha
